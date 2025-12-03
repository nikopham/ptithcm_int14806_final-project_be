package com.ptithcm.movie.user.dto.request;

import lombok.Data;

@Data
public class UserSearchRequest {
    private String query;
    private Boolean isActive;
    private Boolean emailVerified;
}
