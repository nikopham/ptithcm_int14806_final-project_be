package com.ptithcm.movie.user.dto.request;

import lombok.Data;

@Data
public class AdminSearchRequest {
    private String query;
    private Boolean isActive;
    private String roleCode;
}