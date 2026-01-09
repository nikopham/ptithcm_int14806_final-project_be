package com.ptithcm.movie.auth.dto;

public record RegisterRequest(String email, String name, String password, String repassword) {}
