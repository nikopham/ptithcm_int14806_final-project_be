package com.ptithcm.movie.auth.service;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.ptithcm.movie.auth.dto.AuthResponse;
import com.ptithcm.movie.auth.dto.LoginRequest;
import com.ptithcm.movie.auth.dto.RegisterRequest;
import com.ptithcm.movie.auth.entity.VerificationToken;
import com.ptithcm.movie.auth.jwt.JwtTokenProvider;
import com.ptithcm.movie.auth.repository.VerificationTokenRepository;
import com.ptithcm.movie.auth.security.UserPrincipal;
import com.ptithcm.movie.auth.session.UserSessionService;
import com.ptithcm.movie.common.constant.ErrorCode;
import com.ptithcm.movie.common.constant.ErrorMessage;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.config.JwtConfig;
import com.ptithcm.movie.config.MailConfig;
import com.ptithcm.movie.user.entity.User;
import com.ptithcm.movie.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.ptithcm.movie.common.constant.GlobalConstant.RESEND_INTERVAL_SEC;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final VerificationTokenRepository tokenRepo;
    private final PasswordEncoder encoder;
    private final MailConfig mailConfig;
    private final JwtTokenProvider jwtProvider;
    private final JwtConfig jwtConfig;
    private final UserSessionService sessionSvc;

    public ServiceResult register(RegisterRequest r, String baseUrl) {

        /* Step 1: password match */
        if (!r.password().equals(r.repassword()))
            return ServiceResult.Failure()
                    .code(ErrorCode.BAD_CREDENTIALS)
                    .message("Password mismatch");

        Optional<User> opt = userRepo.findByEmailIgnoreCase(r.email());
        User user;

        if (opt.isPresent()) {
            user = opt.get();

            /* Đã xác minh */
            if (Boolean.TRUE.equals(user.getEmailVerified()))
                return ServiceResult.Failure()
                        .code(ErrorCode.EMAIL_EXISTS)
                        .message(ErrorMessage.EMAIL_EXISTS);

            VerificationToken vt = tokenRepo.findByUserId(user.getId()).orElse(null);
            OffsetDateTime now = OffsetDateTime.now();

            /* —— CHẶN gửi lại quá nhanh —— */
            if (vt != null &&              // có token
                    vt.getExpiresAt().isAfter(now) &&               // còn hạn
                    vt.getCreatedAt() != null &&
                    now.minusSeconds(RESEND_INTERVAL_SEC)
                            .isBefore(vt.getCreatedAt())) {              // dưới 60s

                long wait = RESEND_INTERVAL_SEC -
                        java.time.Duration.between(vt.getCreatedAt(), now).getSeconds();

                return ServiceResult.Failure()
                        .code(ErrorCode.TOO_MANY_REQUESTS)
                        .message("Bạn vừa yêu cầu xác nhận. Hãy đợi " + wait + "s rồi thử lại.");
            }

            /* token hết hạn hoặc vượt cooldown → tạo mới */
            if (vt != null) tokenRepo.delete(vt);
            vt = tokenRepo.save(VerificationToken.builder()
                    .token(UUID.randomUUID().toString())
                    .user(user)
                    .expiresAt(now.plusDays(1))
                    .build());

            user.setUsername(r.name());
            user.setPasswordHash(encoder.encode(r.password()));
            userRepo.save(user);

            return sendMailAndReturn(user, vt, baseUrl, "Đã gửi lại link xác nhận");

        } else {
            /* Người dùng mới */
            user = userRepo.save(User.builder()
                    .email(r.email())
                    .username(r.name())
                    .passwordHash(encoder.encode(r.password()))
                    .emailVerified(false)
                    .build());

            VerificationToken vt = tokenRepo.save(VerificationToken.builder()
                    .token(UUID.randomUUID().toString())
                    .user(user)
                    .expiresAt(OffsetDateTime.now().plusDays(1))
                    .build());

            return sendMailAndReturn(user, vt, baseUrl, "Đã gửi link xác nhận vào email");
        }
    }

    /* Helper */
    private ServiceResult sendMailAndReturn(User user,
                                            VerificationToken vt,
                                            String baseUrl,
                                            String successMsg) {

        String link = baseUrl + "/api/auth/verify?token=" + vt.getToken();
        try {
            mailConfig.sendVerification(user.getEmail(), link);
        } catch (Exception e) {
//            log.error("Send mail failed", e);
            return ServiceResult.Failure()
                    .code(ErrorCode.MAIL_SEND_ERROR)
                    .message("Không thể gửi email. Vui lòng thử lại sau");
        }

        return ServiceResult.Success()
                .code(ErrorCode.SUCCESS)
                .message(successMsg);
    }

    /* ------------ VERIFY E-MAIL ------------ */
    public ServiceResult verify(String token) {

        VerificationToken vt = tokenRepo.findByToken(token).orElse(null);
        if (vt == null)
            return ServiceResult.Failure()
                    .code(ErrorCode.BAD_CREDENTIALS)
                    .message("Token invalid");

        if (vt.getExpiresAt().isBefore(OffsetDateTime.now()))
            return ServiceResult.Failure()
                    .code(ErrorCode.BAD_CREDENTIALS)
                    .message("Token expired");

        User user = vt.getUser();
        if (Boolean.TRUE.equals(user.getEmailVerified()))
            return ServiceResult.Success()
                    .code(ErrorCode.SUCCESS)
                    .message("Email đã được xác minh từ trước")
                    .data(user.getEmail());

        /* cập nhật trạng thái */
        user.setEmailVerified(true);
        userRepo.save(user);
        tokenRepo.delete(vt);

        return ServiceResult.Success()
                .code(ErrorCode.SUCCESS)
                .message("Xác nhận email thành công")
                .data(user.getEmail());
    }

    public RedirectView verifyRedirect(String token) {
        ServiceResult result = verify(token);

        String target;
        if (Boolean.TRUE.equals(result.getSuccess())) {
            // success -> frontend page
            target = "https://frontend.host/verify-success?email=" +
                    UriUtils.encode((String) result.getData(), StandardCharsets.UTF_8);
        } else {
            // failure -> show reason
            target = "https://frontend.host/verify-fail?msg=" +
                    UriUtils.encode(result.getMessage(), StandardCharsets.UTF_8);
        }
        return new RedirectView(target);
    }

    /* ------------ LOGIN ------------ */
    public ServiceResult login(LoginRequest r,
                               HttpServletResponse res,
                               String userAgent) {

        /* 1. Kiểm tra email + mật khẩu */
        User user = userRepo.findByEmailIgnoreCase(r.email()).orElse(null);
        if (user == null || !encoder.matches(r.password(), user.getPasswordHash()))
            return ServiceResult.Failure()
                    .code(ErrorCode.BAD_CREDENTIALS)
                    .message(ErrorMessage.BAD_CREDENTIALS);

        /* 2. Chưa xác minh email */
        if (Boolean.FALSE.equals(user.getEmailVerified()))
            return ServiceResult.Failure()
                    .code(ErrorCode.EMAIL_NOT_VERIFIED)
                    .message(ErrorMessage.EMAIL_NOT_VERIFIED);

        /* 3. Tạo session-ID (JTI) trong Redis, để có thể revoke */
        UUID jti = sessionSvc.createSession(user.getId(), userAgent);  // -> lưu TTL = refresh-ttl

        /* 4. Sinh JWT */
        String accessToken  = jwtProvider.generateAccessToken(new UserPrincipal(user));
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), jti);

        /* 5. Ghi Refresh-token vào cookie HttpOnly */
        ResponseCookie cookie = ResponseCookie.from(jwtConfig.getRefreshCookie(), refreshToken)
                .httpOnly(true)
                .secure(true)                      // HTTPS prod
                .path("/api/auth/refresh")         // chỉ gửi cookie cho endpoint refresh
                .maxAge(Duration.ofDays(jwtConfig.getRefreshTtlDay()))
                .sameSite("Lax")
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        /* 6. Trả access-token về body */
        AuthResponse payload = new AuthResponse(accessToken);
        return ServiceResult.Success()
                .code(ErrorCode.SUCCESS)
                .message("Đăng nhập thành công")
                .data(payload);
    }

    public ServiceResult refreshTokens(String refreshToken, HttpServletResponse res) {

        if (refreshToken == null) {
            return ServiceResult.Failure()
                    .code(ErrorCode.BAD_CREDENTIALS)
                    .message("Missing refresh token");
        }

        try {
            DecodedJWT jwtOld = jwtProvider.verify(refreshToken);

            UUID userId = UUID.fromString(jwtOld.getSubject());
            UUID jti    = UUID.fromString(jwtOld.getId());

            /* Kiểm tra JTI còn hợp lệ trong Redis */
            if (!sessionSvc.isActive(userId, jti)) {
                return ServiceResult.Failure()
                        .code(ErrorCode.BAD_CREDENTIALS)
                        .message("Token revoked");
            }

            /* Phát Access mới */
            User user = userRepo.findById(userId).orElseThrow();
            String newAccess = jwtProvider.generateAccessToken(new UserPrincipal(user));

            /* Rotate refresh-token (same jti) */
            String newRefresh = jwtProvider.generateRefreshToken(userId, jti);
            ResponseCookie cookie = ResponseCookie.from(jwtConfig.getRefreshCookie(), newRefresh)
                    .httpOnly(true).secure(true).path("/api/auth/refresh")
                    .maxAge(Duration.ofDays(jwtConfig.getRefreshTtlDay()))
                    .sameSite("Lax").build();
            res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            return ServiceResult.Success()
                    .code(ErrorCode.SUCCESS)
                    .data(new AuthResponse(newAccess));

        } catch (JWTVerificationException ex) {
            return ServiceResult.Failure()
                    .code(ErrorCode.BAD_CREDENTIALS)
                    .message("Invalid refresh token");
        }
    }

    public ServiceResult logout(String refreshToken, HttpServletResponse res) {
        if (refreshToken == null)
            return ServiceResult.Success()
                    .code(ErrorCode.SUCCESS)
                    .message("Logged out");

        try {
            DecodedJWT jwtOld = jwtProvider.verify(refreshToken);
            UUID userId = UUID.fromString(jwtOld.getSubject());
            UUID jti    = UUID.fromString(jwtOld.getId());

            sessionSvc.revoke(userId, jti);              // xoá key trên Redis

        } catch (JWTVerificationException ignored) {
            // token sai chữ ký/hết hạn ⇒ bỏ qua
        }

        /* dọn cookie refresh_token */
        ResponseCookie clear = ResponseCookie.from(jwtConfig.getRefreshCookie(), "")
                .httpOnly(true).secure(true)
                .path("/api/auth/refresh")
                .maxAge(0)          // xoá
                .sameSite("Lax").build();
        res.addHeader(HttpHeaders.SET_COOKIE, clear.toString());

        return ServiceResult.Success()
                .code(ErrorCode.SUCCESS)
                .message("Đăng xuất thành công");
    }

    public void handleGoogleLogin(OAuth2AuthenticationToken token,
                                  HttpServletRequest req,
                                  HttpServletResponse res) throws IOException {

        Map<String, Object> attr = token.getPrincipal().getAttributes();
        String email     = (String)  attr.get("email");
        String fullName  = (String)  attr.get("name");
        boolean verified = Boolean.TRUE.equals(attr.get("email_verified"));

        /* -- find or create user ------------------------------------------------ */
        User user = userRepo.findByEmailIgnoreCase(email)
                .orElseGet(() -> userRepo.save(User.builder()
                        .email(email)
                        .username(fullName)
                        .emailVerified(verified)
                        .passwordHash("")          // không dùng mật khẩu
                        .build()));

        /* -- create session (JTI) + JWT ---------------------------------------- */
        UUID jti = sessionSvc.createSession(user.getId(), req.getHeader("User-Agent"));
        String access  = jwtProvider.generateAccessToken(new UserPrincipal(user));
        String refresh = jwtProvider.generateRefreshToken(user.getId(), jti);

        /* -- write refresh-token cookie ---------------------------------------- */
        ResponseCookie cookie = ResponseCookie.from(jwtConfig.getRefreshCookie(), refresh)
                .httpOnly(true).secure(true).path("/api/auth/refresh")
                .maxAge(Duration.ofDays(jwtConfig.getRefreshTtlDay()))
                .sameSite("Lax").build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        /* -- redirect Front-end ------------------------------------------------ */
        res.sendRedirect("https://frontend.host/oauth2/success?token=" + access);
    }
}