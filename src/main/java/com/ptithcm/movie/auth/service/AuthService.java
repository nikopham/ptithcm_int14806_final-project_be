package com.ptithcm.movie.auth.service;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.ptithcm.movie.auth.dto.*;
import com.ptithcm.movie.auth.entity.AuthProvider;
import com.ptithcm.movie.auth.entity.PasswordResetToken;
import com.ptithcm.movie.auth.entity.UserOauthAccount;
import com.ptithcm.movie.auth.entity.VerificationToken;
import com.ptithcm.movie.auth.jwt.JwtTokenProvider;
import com.ptithcm.movie.auth.repository.AuthProviderRepository;
import com.ptithcm.movie.auth.repository.PasswordResetTokenRepository;
import com.ptithcm.movie.auth.repository.UserOAuthAccountRepository;
import com.ptithcm.movie.auth.repository.VerificationTokenRepository;
import com.ptithcm.movie.auth.security.UserPrincipal;
import com.ptithcm.movie.auth.session.UserSessionService;
import com.ptithcm.movie.common.constant.ErrorCode;
import com.ptithcm.movie.common.constant.ErrorMessage;
import com.ptithcm.movie.common.constant.GlobalConstant;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.config.JwtConfig;
import com.ptithcm.movie.config.MailConfig;
import com.ptithcm.movie.user.entity.Role;
import com.ptithcm.movie.user.entity.User;
import com.ptithcm.movie.user.repository.RoleRepository;
import com.ptithcm.movie.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

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
    private final PasswordResetTokenRepository prRepo;
    private final MailService mailService;
    private final RoleRepository roleRepo;
    private final GoogleHelper googleHelper;
    private final AuthProviderRepository providerRepository;
    private final UserOAuthAccountRepository oauthRepository;


    @Transactional
    public ServiceResult loginByGoogle(GoogleLoginRequest request, String userAgent) {
        try {
            // 1. Xác thực với Google Server (Lấy thông tin User)
            GoogleIdToken.Payload payload = googleHelper.verify(request.getCode());

            String email = payload.getEmail();
            String googleUserId = payload.getSubject();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            // 2. Tìm hoặc Tạo User (Logic xử lý liên kết tài khoản)
            User user = resolveGoogleUser(email, googleUserId, name, pictureUrl);

            // 3. Check trạng thái (Giống hàm login thường)
            if (Boolean.FALSE.equals(user.isActive())) {
                return ServiceResult.Failure()
                        .code(ErrorCode.BANNED_ACCOUNT)
                        .message("Tài khoản của bạn đã bị khóa");
            }

            // 4. Tạo Session & Token (Copy logic từ hàm login)
            // Tạo JTI cho session quản lý thiết bị
            UUID jti = sessionSvc.createSession(user.getId(), userAgent);

            UserPrincipal userPrincipal = new UserPrincipal(user);
            String accessToken = jwtProvider.generateAccessToken(userPrincipal);
            String refreshToken = jwtProvider.generateRefreshToken(user.getId(), jti);

            // 5. Build Response (Đồng bộ cấu trúc với login thường)
            List<String> listRoles = new ArrayList<>();
            if (user.getRole() != null) {
                listRoles.add(user.getRole().getCode());
            }

            AuthResponse authResponse = new AuthResponse();

            authResponse.setAccessToken(accessToken);
            authResponse.setUsername(user.getUsername());
            authResponse.setAvatarUrl(user.getAvatarUrl());
            authResponse.setRoles(listRoles);

            LoginResult result = LoginResult.builder()
                    .refreshToken(refreshToken) // Trả về để FE lưu cookie hoặc storage
                    .authResponse(authResponse)
                    .build();

            return ServiceResult.Success()
                    .code(ErrorCode.SUCCESS)
                    .message("Đăng nhập Google thành công")
                    .data(result);

        } catch (Exception e) {
            log.error("Google Login Error", e);
            return ServiceResult.Failure()
                    .code(ErrorCode.BAD_CREDENTIALS) // Hoặc mã lỗi riêng
                    .message("Đăng nhập Google thất bại: " + e.getMessage());
        }
    }


    private User resolveGoogleUser(String email, String googleUserId, String name, String avatarUrl) {
        AuthProvider provider = providerRepository.findByProviderKey("google")
                .orElseThrow(() -> new RuntimeException("Google provider chưa được cấu hình trong DB"));

        // Tìm xem đã có liên kết OAuth chưa?
        Optional<UserOauthAccount> oauthOpt = oauthRepository
                .findByProvider_IdAndProviderUserId(Integer.valueOf(provider.getId()), googleUserId);

        if (oauthOpt.isPresent()) {
            // Đã từng đăng nhập Google -> Trả về User cũ
            User user = oauthOpt.get().getUser();
            // Update lại avatar nếu user chưa có avatar
            if (user.getAvatarUrl() == null && avatarUrl != null) {
                user.setAvatarUrl(avatarUrl);
                userRepo.save(user);
            }
            return user;
        }

        // Chưa liên kết -> Tìm theo Email
        Optional<User> userByEmailOpt = userRepo.findByEmailIgnoreCase(email);

        if (userByEmailOpt.isPresent()) {
            // Email đã tồn tại (User đăng ký thường trước đó) -> Link tài khoản
            User existingUser = userByEmailOpt.get();
            linkGoogleAccount(existingUser, provider, googleUserId, email);

            // Google login coi như đã verify email
            if (!Boolean.TRUE.equals(existingUser.isEmailVerified())) {
                existingUser.setEmailVerified(true);
                userRepo.save(existingUser);
            }
            return existingUser;
        }

        // User hoàn toàn mới -> Tạo User + Link
        User newUser = createNewGoogleUser(email, name, avatarUrl);
        linkGoogleAccount(newUser, provider, googleUserId, email);
        return newUser;
    }

    private User createNewGoogleUser(String email, String name, String avatarUrl) {
        Role userRole = roleRepo.findByCode("viewer")
                .orElseThrow(() -> new RuntimeException("Role USER not found"));

        User newUser = new User();
        newUser.setEmail(email);
        // Tạo username từ email
        newUser.setUsername(email.split("@")[0]);
        newUser.setRole(userRole);
        newUser.setAvatarUrl(avatarUrl);
        newUser.setEmailVerified(true);
        newUser.setActive(true);
        newUser.setImported(false);

        return userRepo.save(newUser);
    }

    private void linkGoogleAccount(User user, AuthProvider provider, String googleUserId, String email) {
        UserOauthAccount oauth = new UserOauthAccount();
        oauth.setUser(user);
        oauth.setProvider(provider);
        oauth.setProviderUserId(googleUserId);
        oauth.setEmail(email);
        oauth.setEmailVerified(true);

        oauthRepository.save(oauth);
    }

    /* yêu cầu reset --------------------------------------------------- */
    public ServiceResult forgotPassword(ForgotPasswordRequest req, String baseUrl) {

        User user = userRepo.findByEmailIgnoreCase(req.email()).orElse(null);
        if (user == null)
            return ServiceResult.Success()
                    .message("Nếu email tồn tại, chúng tôi đã gửi hướng dẫn.");

        OffsetDateTime now = OffsetDateTime.now();
        PasswordResetToken pr = prRepo.findByUserId(user.getId()).orElse(null);

        if (pr != null &&
                now.minusSeconds(GlobalConstant.RESEND_INTERVAL_SEC).isBefore(pr.getCreatedAt())) {

            long wait = GlobalConstant.RESEND_INTERVAL_SEC -
                    Duration.between(pr.getCreatedAt(), now).getSeconds();

            return ServiceResult.Failure()
                    .code(ErrorCode.RESET_TOO_MANY)
                    .message("Vui lòng đợi " + wait + "giây rồi thử lại.");
        }

        if (pr != null) prRepo.delete(pr);
        pr = prRepo.save(PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(now.plusHours(2))
                .build());

//        String link = "http://localhost:5173" + "/reset-password?token=" + pr.getToken();

        String link = "https://ptithcm-int-14806-final-project-43upmrm53.vercel.app" + "/reset-password?token=" + pr.getToken();
        return mailService.sendPasswordReset(user, link);   // trả ServiceResult luôn
    }

    /* verify token ---------------------------------------------------- */
    public ServiceResult verifyResetToken(String token) {
        PasswordResetToken pr = prRepo.findByToken(token).orElse(null);
        if (pr == null || pr.getExpiresAt().isBefore(OffsetDateTime.now()))
            return ServiceResult.Failure()
                    .code(ErrorCode.RESET_TOKEN_INVALID)
                    .message("Token hết hạn hoặc không hợp lệ!");
        return ServiceResult.Success().message("Token hợp lệ");
    }

    /* lưu mật khẩu mới ------------------------------------------------- */
    public ServiceResult resetPassword(ResetPasswordRequest req) {

        if (!req.password().equals(req.repassword()))
            return ServiceResult.Failure()
                    .code(ErrorCode.BAD_CREDENTIALS)
                    .message("Mật khẩu không khớp");

        PasswordResetToken pr = prRepo.findByToken(req.token()).orElse(null);
        if (pr == null || pr.getExpiresAt().isBefore(OffsetDateTime.now()))
            return ServiceResult.Failure()
                    .code(ErrorCode.RESET_TOKEN_INVALID)
                    .message("Token hết hạn hoặc không hợp lệ!");

        User user = pr.getUser();
        user.setPasswordHash(encoder.encode(req.password()));
        userRepo.save(user);
        prRepo.delete(pr);

        return ServiceResult.Success().message("Đặt lại mật khẩu thành công");
    }

    public ServiceResult register(RegisterRequest r, String baseUrl) {

        /* Step 1: password match */
        if (!r.password().equals(r.repassword()))
            return ServiceResult.Failure()
                    .code(ErrorCode.BAD_CREDENTIALS)
                    .message("Mật khẩu không khớp");

        Role viewerRole = roleRepo.findByCode("viewer")
                .orElseThrow(() -> new IllegalStateException("Lỗi dữ liệu."));

        Optional<User> opt = userRepo.findByEmailIgnoreCase(r.email());
        User user;

        if (opt.isPresent()) {
            user = opt.get();

            /* Đã xác minh */
            if (Boolean.TRUE.equals(user.isEmailVerified()))
                return ServiceResult.Failure()
                        .code(ErrorCode.EMAIL_EXISTS)
                        .message(ErrorMessage.EMAIL_EXISTS);

            VerificationToken vt = tokenRepo.findByUserId(user.getId()).orElse(null);
            OffsetDateTime now = OffsetDateTime.now();

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
            user.setActive(false);
            user.setPasswordHash(encoder.encode(r.password()));
            if (user.getRole() == null) user.setRole(viewerRole);
            userRepo.save(user);

            String link = baseUrl + "/api/auth/verifyRedirect?token=" + vt.getToken();
            return mailService.sendRegisterVerification(user, link);

        } else {
            /* Người dùng mới */
            user = userRepo.save(User.builder()
                    .email(r.email())
                    .username(r.name())
                    .passwordHash(encoder.encode(r.password()))
                    .emailVerified(false)
                    .role(viewerRole)
                    .isActive(false)
                    .build());

            VerificationToken vt = tokenRepo.save(VerificationToken.builder()
                    .token(UUID.randomUUID().toString())
                    .user(user)
                    .expiresAt(OffsetDateTime.now().plusDays(1))
                    .build());

            String link = baseUrl + "/api/auth/verifyRedirect?token=" + vt.getToken();
            return mailService.sendRegisterVerification(user, link);
        }
    }

    /* ------------ VERIFY E-MAIL ------------ */
    public ServiceResult verify(String token) {

        VerificationToken vt = tokenRepo.findByToken(token).orElse(null);
        if (vt == null)
            return ServiceResult.Failure()
                    .code(ErrorCode.BAD_CREDENTIALS)
                    .message("Token không hợp lệ");

        if (vt.getExpiresAt().isBefore(OffsetDateTime.now()))
            return ServiceResult.Failure()
                    .code(ErrorCode.BAD_CREDENTIALS)
                    .message("Token hết hạn");

        User user = vt.getUser();
        if (Boolean.TRUE.equals(user.isEmailVerified()))
            return ServiceResult.Success()
                    .code(ErrorCode.SUCCESS)
                    .message("Email đã được xác minh từ trước")
                    .data(user.getEmail());

        /* cập nhật trạng thái */
        user.setEmailVerified(true);
        user.setActive(true);
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
            target = "https://ptithcm-int-14806-final-project-43upmrm53.vercel.app/verify-success?email=" +
                    UriUtils.encode((String) result.getData(), StandardCharsets.UTF_8);
//            target = "http://localhost:5173/verify-success?email=" +
//                    UriUtils.encode((String) result.getData(), StandardCharsets.UTF_8);
        } else {
            // failure -> show reason
            target = "https://ptithcm-int-14806-final-project-43upmrm53.vercel.app/verify-fail?msg=" +
                    UriUtils.encode(result.getMessage(), StandardCharsets.UTF_8);
//            target = "http://localhost:5173/verify-fail?msg=" +
//                    UriUtils.encode(result.getMessage(), StandardCharsets.UTF_8);
        }
        return new RedirectView(target);
    }

    /* ------------ LOGIN ------------ */
    public ServiceResult login(LoginRequest r,

                               String userAgent) {

        // 1. Kiểm tra email (Tìm user)
        User user = userRepo.findByEmailIgnoreCase(r.email()).orElse(null);

        // Check password
        if (user == null || !encoder.matches(r.password(), user.getPasswordHash()))
            return ServiceResult.Failure()
                    .code(ErrorCode.BAD_CREDENTIALS)
                    .message("Email hoặc mật khẩu không chính xác"); // Message thân thiện hơn

        // 2. Check trạng thái
        if (Boolean.FALSE.equals(user.isEmailVerified()))
            return ServiceResult.Failure()
                    .code(ErrorCode.EMAIL_NOT_VERIFIED)
                    .message("Vui lòng xác thực email trước khi đăng nhập");

        if (Boolean.FALSE.equals(user.isActive()))
            return ServiceResult.Failure()
                    .code(ErrorCode.BANNED_ACCOUNT)
                    .message("Tài khoản của bạn đã bị khóa");

        UUID jti = sessionSvc.createSession(user.getId(), userAgent);

        String accessToken  = jwtProvider.generateAccessToken(new UserPrincipal(user));
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), jti);

//        boolean isSecure = jwtConfig.isCookieSecure();
//
//        ResponseCookie cookie = ResponseCookie.from(jwtConfig.getRefreshCookie(), refreshToken)
//                .httpOnly(true)
//                .secure(isSecure)
//                .path("/api/auth/refresh")
//                .maxAge(Duration.ofDays(jwtConfig.getRefreshTtlDay()))
//                .sameSite("Lax")
//                .build();
//
//        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        List<String> listRoles = new ArrayList<>();
        if (user.getRole() != null) {
            listRoles.add(user.getRole().getCode());
        }

        AuthResponse payload = new AuthResponse();

        payload.setAccessToken(accessToken);
        payload.setUsername(user.getUsername());
        payload.setAvatarUrl(user.getAvatarUrl());
        payload.setRoles(listRoles);

        LoginResult result = LoginResult.builder()
                .refreshToken(refreshToken)
                .authResponse(payload)
                .build();

        return ServiceResult.Success()
                .code(ErrorCode.SUCCESS)
                .message("Đăng nhập thành công")
                .data(result);
    }

    public ServiceResult refreshTokens(String refreshToken) {

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
            User user = userRepo.findById(userId).orElse(null);
            if (user == null) {
                return ServiceResult.Failure()
                        .code(ErrorCode.BAD_CREDENTIALS)
                        .message("Người dùng không tồn tại");
            }
            String newAccess = jwtProvider.generateAccessToken(new UserPrincipal(user));

            /* Rotate refresh-token (same jti) */
            String newRefresh = jwtProvider.generateRefreshToken(userId, jti);
//            ResponseCookie cookie = ResponseCookie.from(jwtConfig.getRefreshCookie(), newRefresh)
//                    .httpOnly(true).secure(jwtConfig.isCookieSecure()).path("/api/auth/refresh")
//                    .maxAge(Duration.ofDays(jwtConfig.getRefreshTtlDay()))
//                    .sameSite("Lax").build();
//            res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            List<String> listRoles = new ArrayList<>();
            listRoles.add(user.getRole().getCode());

            AuthResponse payload = new AuthResponse();

            payload.setAccessToken(newAccess);
            payload.setUsername(user.getUsername());
            payload.setAvatarUrl(user.getAvatarUrl());
            payload.setRoles(listRoles);

            LoginResult result = LoginResult.builder()
                    .refreshToken(newRefresh)
                    .authResponse(payload)
                    .build();
            return ServiceResult.Success()
                    .data(result)
                    .code(ErrorCode.SUCCESS);

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
                    .message("Đã đăng xuất");

        try {
            DecodedJWT jwtOld = jwtProvider.verify(refreshToken);
            UUID userId = UUID.fromString(jwtOld.getSubject());
            UUID jti    = UUID.fromString(jwtOld.getId());

            sessionSvc.revoke(userId, jti);              // xoá key trên Redis

        } catch (JWTVerificationException ignored) {
            return ServiceResult.Success()
                    .code(ErrorCode.SUCCESS)
                    .message("Đã có lỗi xảy ra");
        }
        return ServiceResult.Success()
                .code(ErrorCode.SUCCESS)
                .message("Thu hồi token thành công");




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
                .sameSite("None").build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        /* -- redirect Front-end ------------------------------------------------ */
        res.sendRedirect("https://frontend.host/oauth2/success?token=" + access);
    }
}