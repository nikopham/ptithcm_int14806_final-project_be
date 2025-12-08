package com.ptithcm.movie.auth.dto;

import lombok.Data;

@Data
public class GoogleLoginRequest {
    private String code;
}
