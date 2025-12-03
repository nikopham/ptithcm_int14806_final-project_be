package com.ptithcm.movie.auth.controller;

import com.ptithcm.movie.auth.dto.*;
import com.ptithcm.movie.auth.service.AuthService;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.config.JwtConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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

        ResponseCookie cookie = ResponseCookie.from(jwtConfig.getRefreshCookie(), loginData.getRefreshToken())
                .httpOnly(true)
                .secure(jwtConfig.isCookieSecure())
                .path("/api/auth/refresh")
                .maxAge(Duration.ofDays(jwtConfig.getRefreshTtlDay()))
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, String.valueOf(cookie))
                .body(ServiceResult.Success()
                        .data(loginData.getAuthResponse()));
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

        ResponseCookie cookie = ResponseCookie.from(jwtConfig.getRefreshCookie(), loginData.getRefreshToken())
            .httpOnly(true)
            .secure(jwtConfig.isCookieSecure())
            .path("/api/auth/refresh")
            .maxAge(Duration.ofDays(jwtConfig.getRefreshTtlDay()))
            .sameSite("Lax")
            .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, String.valueOf(cookie))
                .body(ServiceResult.Success()
                        .data(loginData.getAuthResponse()));
    }

    @PostMapping("/logout")
    public ServiceResult logout(@CookieValue(value = "${app.jwt.refresh-cookie}", required = false)
                                String refreshToken,
                                HttpServletResponse response) {

        return authService.logout(refreshToken, response);
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
}