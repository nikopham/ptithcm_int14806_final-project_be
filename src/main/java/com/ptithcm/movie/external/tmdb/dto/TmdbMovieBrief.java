package com.ptithcm.movie.external.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbMovieBrief(
        @JsonProperty("id")          Integer id,
        @JsonProperty("title")       String title,
        @JsonProperty("release_date") String releaseDate,
        @JsonProperty("poster_path") String posterPath) {}
