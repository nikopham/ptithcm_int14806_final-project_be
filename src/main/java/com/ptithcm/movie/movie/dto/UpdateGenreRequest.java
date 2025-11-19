package com.ptithcm.movie.movie.dto;

import lombok.Data;

@Data
public class UpdateGenreRequest {
    private String name;
    private Integer tmdbId; // Có thể null
}
