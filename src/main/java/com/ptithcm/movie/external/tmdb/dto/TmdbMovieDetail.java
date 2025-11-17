package com.ptithcm.movie.external.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbMovieDetail(
        Integer id,
        String title,
        String overview,
        @JsonProperty("release_date") String releaseDate,
        @JsonProperty("runtime")      Integer runtime,
        @JsonProperty("poster_path")  String posterPath) {}