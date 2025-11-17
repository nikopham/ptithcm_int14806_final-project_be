package com.ptithcm.movie.external.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CastMemberDto(int id, String name, String character, @JsonProperty("profile_path") String profilePath) {}
