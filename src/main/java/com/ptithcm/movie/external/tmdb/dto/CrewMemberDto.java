package com.ptithcm.movie.external.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CrewMemberDto(int id, String name, String job, @JsonProperty("profile_path") String profilePath) {}
