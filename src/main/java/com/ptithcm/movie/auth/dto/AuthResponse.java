package com.ptithcm.movie.auth.dto;

import java.util.List;

public record AuthResponse(String accessToken, String username, String avatarUrl, List<String> roles) {}
