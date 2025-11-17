package com.ptithcm.movie.external.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MovieItemDto(
        int id,
        String title,
        String overview,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("release_date") String releaseDate,
        @JsonProperty("vote_average") double voteAverage
) {}