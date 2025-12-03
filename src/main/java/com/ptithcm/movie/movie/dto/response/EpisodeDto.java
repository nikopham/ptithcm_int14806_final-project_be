package com.ptithcm.movie.movie.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class EpisodeDto {
    private UUID id;
    private Integer episodeNumber;
    private String title;
    private Integer durationMin;
    private String synopsis;
    private LocalDate airDate;
    private String stillPath;
}