package com.ptithcm.movie.external.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EpisodeDto(
        @JsonProperty("id") int tmdbId,
        String name, // -> title
        String overview, // -> synopsis
        @JsonProperty("air_date") String airDate,
        @JsonProperty("episode_number") int episodeNumber,
        @JsonProperty("still_path") String stillPath,
        int runtime // -> duration_min
) {}
