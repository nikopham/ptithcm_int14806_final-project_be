package com.ptithcm.movie.external.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CountryDto(@JsonProperty("iso_3166_1") String code, String name) {}
