package com.ptithcm.movie.external.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// Map kết quả từ /search/tv
public record TvSearchResultDto(
        int page,
        List<TvSearchItemDto> results,
        @JsonProperty("total_pages") int totalPages
) {}