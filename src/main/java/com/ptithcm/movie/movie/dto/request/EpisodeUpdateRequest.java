package com.ptithcm.movie.movie.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;
import java.time.LocalDate;

@Data
public class EpisodeUpdateRequest {
    @Min(1)
    private Integer episodeNumber;
    private String title;
    private Integer durationMin;
    private String synopsis;
    private LocalDate airDate;
}