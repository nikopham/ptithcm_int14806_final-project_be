package com.ptithcm.movie.movie.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EpisodeCreateRequest {
    @NotNull(message = "Episode number is required")
    @Min(value = 1, message = "Episode number must be at least 1")
    private Integer episodeNumber;

    @NotBlank(message = "Title is required")
    private String title;

    private Integer durationMin;

    private String synopsis;

    private LocalDate airDate;
}