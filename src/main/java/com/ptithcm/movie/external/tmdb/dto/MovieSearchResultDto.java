package com.ptithcm.movie.external.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// Dùng 'record' cho DTO tìm kiếm
public record MovieSearchResultDto(
        int page,
        List<MovieItemDto> results,
        @JsonProperty("total_pages") int totalPages
) {}