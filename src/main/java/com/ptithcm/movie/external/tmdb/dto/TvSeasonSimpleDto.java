package com.ptithcm.movie.external.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TvSeasonSimpleDto(
        int id,
        String name,
        @JsonProperty("season_number") int seasonNumber,
        @JsonProperty("episode_count") int episodeCount
) {}
