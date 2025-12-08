package com.ptithcm.movie.auth.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Setter
@Getter
public class AuthResponse {

    private String accessToken;
    private String username;
    private String avatarUrl;
    private List<String> roles;
}
