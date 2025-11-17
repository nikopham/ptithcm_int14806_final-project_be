package com.ptithcm.movie.auth.controller;

import com.ptithcm.movie.auth.dto.ForgotPasswordRequest;
import com.ptithcm.movie.auth.dto.LoginRequest;
import com.ptithcm.movie.auth.dto.RegisterRequest;
import com.ptithcm.movie.auth.dto.ResetPasswordRequest;
import com.ptithcm.movie.auth.service.AuthService;
import com.ptithcm.movie.common.dto.ServiceResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.base-url:http://localhost:5173}")
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
    public ServiceResult login(@RequestBody LoginRequest req,
                               HttpServletRequest request,
                               HttpServletResponse response) {

        String ua = request.getHeader("User-Agent");
        return authService.login(req, response, ua);
    }

    @PostMapping("/refresh")
    public ServiceResult refresh(@CookieValue(value = "${app.jwt.refresh-cookie}", required = false)
                                 String refreshToken,
                                 HttpServletResponse response) {

        return authService.refreshTokens(refreshToken, response);
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