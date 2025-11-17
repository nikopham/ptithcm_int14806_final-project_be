package com.ptithcm.movie.external.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TvSeasonDetailDto(
        @JsonProperty("season_number") int seasonNumber,
        List<EpisodeDto> episodes // Danh sách episodes
) {}
