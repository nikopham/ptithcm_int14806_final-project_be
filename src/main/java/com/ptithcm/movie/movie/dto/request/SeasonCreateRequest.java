package com.ptithcm.movie.movie.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeasonCreateRequest {
    @NotNull(message = "Season number is required")
    @Min(value = 1, message = "Season number must be at least 1")
    private Integer seasonNumber;

    private String title; // VD: "Season 1: The Beginning"
    private String description;
}