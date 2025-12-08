package com.ptithcm.movie.movie.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieSearchResponse {
    private UUID id;
    private String title;
    private String originalTitle;
    private String description;
    private String slug;

    private String posterUrl;
    private String backdropUrl;

    private String videoUrl;
    private LocalDate releaseDate;
    private Integer releaseYear;
    private Integer durationMin;
    private String ageRating;
    private String quality;
    private String status;
    private boolean isSeries;

}