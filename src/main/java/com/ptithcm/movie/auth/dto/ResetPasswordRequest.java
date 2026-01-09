package com.ptithcm.movie.auth.dto;

public record ResetPasswordRequest(String token,
                                   String password,
                                   String repassword) { }
