package com.ptithcm.movie.movie.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenreRequest {
    @NotBlank(message = "Genre name is required")
    private String name;
}