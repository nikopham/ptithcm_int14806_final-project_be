package com.ptithcm.movie.movie.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private UUID id;

    private UUID movieId;
    private String movieTitle;
    private String moviePosterUrl;

    private UUID userId;
    private String username;
    private String userAvatar;
    private Boolean isHidden;

    private BigDecimal rating;
    private String title;
    private String body;
    private OffsetDateTime createdAt;
}