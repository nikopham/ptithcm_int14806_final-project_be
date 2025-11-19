package com.ptithcm.movie.movie.dto;

import lombok.Data;

@Data
public class CreateGenreRequest {
    private String name;
    private Integer tmdbId; // Có thể null nếu là Custom Genre
}
