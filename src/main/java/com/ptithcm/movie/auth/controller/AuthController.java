package com.ptithcm.movie.auth.controller;

import com.ptithcm.movie.auth.dto.*;
import com.ptithcm.movie.auth.service.AuthService;
import com.ptithcm.movie.common.constant.ErrorCode;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.config.JwtConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtConfig jwtConfig;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @PostMapping("/register")
    public ServiceResult register(@RequestBody RegisterRequest req) {
        return authService.register(req, baseUrl);
    }

    @GetMapping("/verifyRedirect")
    public RedirectView verify(@RequestParam String token) {
        return authService.verifyRedirect(token);
    }

    @PostMapping("/login")
    public ResponseEntity<ServiceResult> login(@RequestBody LoginRequest req,
                               HttpServletRequest request) {

        String ua = request.getHeader("User-Agent");
        ServiceResult result = authService.login(req, ua);

        if (!result.getSuccess()) {
            return ResponseEntity.status(result.getCode() == 401 ? 401 : 400)
                    .body(ServiceResult.Failure().message(result.getMessage()));
        }

        LoginResult loginData = (LoginResult) result.getData();
        AuthResponse authResponse = loginData.getAuthResponse();

        ResponseCookie accessCookie = ResponseCookie.from(jwtConfig.getAccessCookie(), authResponse.getAccessToken())
                .httpOnly(true)
                .secure(jwtConfig.isCookieSecure())
                .path("/")
                .maxAge(Duration.ofDays(jwtConfig.getAccessTtlMin()))
                .sameSite("Lax")
                .build();

        ResponseCookie cookie = ResponseCookie.from(jwtConfig.getRefreshCookie(), loginData.getRefreshToken())
                .httpOnly(true)
                .secure(jwtConfig.isCookieSecure())
                .path("/api/auth/refresh")
                .maxAge(Duration.ofDays(jwtConfig.getRefreshTtlDay()))
                .sameSite("Lax")
                .build();

        authResponse.setAccessToken("");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, String.valueOf(cookie))
                .header(HttpHeaders.SET_COOKIE, String.valueOf(accessCookie))
                .body(ServiceResult.Success()
                        .data(authResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ServiceResult> refresh(@CookieValue(value = "${app.jwt.refresh-cookie}", required = false)
                                 String refreshToken) {

        ServiceResult result = authService.refreshTokens(refreshToken);
        if (!result.getSuccess()) {
            ResponseCookie cleanCookie = ResponseCookie.from("refresh_token", "")
                    .path("/api/auth/refresh").maxAge(0).build();

            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                    .body(ServiceResult.Failure().message(result.getMessage()));
        }

        LoginResult loginData = (LoginResult) result.getData();
        AuthResponse authResponse = loginData.getAuthResponse();
        ResponseCookie accessCookie = ResponseCookie.from(jwtConfig.getAccessCookie(), authResponse.getAccessToken())
                .httpOnly(true)
                .secure(jwtConfig.isCookieSecure())
                .path("/")
                .maxAge(Duration.ofDays(jwtConfig.getAccessTtlMin()))
                .sameSite("Lax")
                .build();

        ResponseCookie cookie = ResponseCookie.from(jwtConfig.getRefreshCookie(), loginData.getRefreshToken())
            .httpOnly(true)
            .secure(jwtConfig.isCookieSecure())
            .path("/api/auth/refresh")
            .maxAge(Duration.ofDays(jwtConfig.getRefreshTtlDay()))
            .sameSite("Lax")
            .build();

        authResponse.setAccessToken("");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, String.valueOf(cookie))
                .header(HttpHeaders.SET_COOKIE, String.valueOf(accessCookie))
                .body(ServiceResult.Success()
                        .data(authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ServiceResult> logout(@CookieValue(value = "${app.jwt.refresh-cookie}", required = false)
                                String refreshToken,
                                HttpServletResponse response) {

        ServiceResult result = authService.logout(refreshToken, response);

        if (!result.getSuccess()) {
            return ResponseEntity.status(result.getCode() == 401 ? 401 : 400)
                    .body(ServiceResult.Failure().message(result.getMessage()));
        }

        ResponseCookie accessCookie = ResponseCookie.from(jwtConfig.getAccessCookie(), "")
                .httpOnly(true)
                .secure(jwtConfig.isCookieSecure())
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        /* dọn cookie refresh_token */
        ResponseCookie clear = ResponseCookie.from(jwtConfig.getRefreshCookie(), "")
                .httpOnly(true).secure(true)
                .path("/api/auth/refresh")
                .maxAge(0)
                .sameSite("Lax").build();


        SecurityContextHolder.clearContext();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clear.toString())
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .body(ServiceResult.Success().message("Đăng xuất thành công"));
    }

    /* POST /api/auth/forgot-password */
    @PostMapping("/forgot-password")
    public ServiceResult forgot(@RequestBody ForgotPasswordRequest req) {
        return authService.forgotPassword(req, baseUrl);
    }

    /* GET  /api/auth/reset/verify?token=xxx */
    @GetMapping("/reset/verify")
    public ServiceResult verifyReset(@RequestParam String token) {
        return authService.verifyResetToken(token);
    }

    /* POST /api/auth/reset */
    @PostMapping("/reset")
    public ServiceResult reset(@RequestBody ResetPasswordRequest req) {
        return authService.resetPassword(req);
    }

    @PostMapping("/google")
    public ResponseEntity<ServiceResult> loginByGoogle(
            @RequestBody GoogleLoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        String userAgent = servletRequest.getHeader(HttpHeaders.USER_AGENT);

        ServiceResult result = authService.loginByGoogle(request, userAgent);

        AuthResponse authResponse = new AuthResponse();
        if (result.getCode() == ErrorCode.SUCCESS) {
            LoginResult loginResult = (LoginResult) result.getData();

            authResponse = loginResult.getAuthResponse();
            ResponseCookie accessCookie = ResponseCookie.from(jwtConfig.getAccessCookie(), authResponse.getAccessToken())
                    .httpOnly(true)
                    .secure(jwtConfig.isCookieSecure())
                    .path("/")
                    .maxAge(Duration.ofDays(jwtConfig.getAccessTtlMin()))
                    .sameSite("Lax")
                    .build();

            ResponseCookie cookie = ResponseCookie.from(jwtConfig.getRefreshCookie(), loginResult.getRefreshToken())
                    .httpOnly(true)
                    .secure(jwtConfig.isCookieSecure())
                    .path("/api/auth/refresh")
                    .maxAge(Duration.ofDays(jwtConfig.getRefreshTtlDay()))
                    .sameSite("Lax")
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE, String.valueOf(accessCookie));
        }
        authResponse.setAccessToken("");
        return ResponseEntity.ok(ServiceResult.Success()
                .data(authResponse));
    }
}