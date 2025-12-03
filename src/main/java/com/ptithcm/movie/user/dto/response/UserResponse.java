package com.ptithcm.movie.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String username;
    private String email;
    private String avatarUrl;
    private String roleCode;
    private boolean emailVerified;
    private boolean isActive;
    private boolean isImported;
    private OffsetDateTime createdAt;
    private Long reviewCount;
    private Long commentCount;
    private Long createdMovieCount;
    private Long updatedMovieCount;
    private Long adminCommentCount;
}
