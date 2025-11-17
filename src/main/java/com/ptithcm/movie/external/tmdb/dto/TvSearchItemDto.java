package com.ptithcm.movie.external.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TvSearchItemDto(
        int id,
        String name, // (TV dùng 'name')
        @JsonProperty("first_air_date") String firstAirDate,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("vote_average") double voteAverage
) {}
