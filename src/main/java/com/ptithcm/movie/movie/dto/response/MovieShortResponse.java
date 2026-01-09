package com.ptithcm.movie.movie.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class MovieShortResponse {
    private UUID id;
    private String title;
    private String originalTitle;
    private String posterUrl;
    private String backdropUrl;
    private String slug;
    private Double imdbScore;
    private Integer releaseYear;
}