package com.ptithcm.movie.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResult {
    private String refreshToken;
    private AuthResponse authResponse;
}
