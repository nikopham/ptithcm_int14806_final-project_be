package com.ptithcm.movie.movie.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SeasonDto {
    private UUID id;
    private Integer seasonNumber;
    private String title;
    private List<EpisodeDto> episodes;
}