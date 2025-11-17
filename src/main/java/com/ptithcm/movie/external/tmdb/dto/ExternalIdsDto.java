package com.ptithcm.movie.external.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExternalIdsDto(@JsonProperty("imdb_id") String imdbId) {}
