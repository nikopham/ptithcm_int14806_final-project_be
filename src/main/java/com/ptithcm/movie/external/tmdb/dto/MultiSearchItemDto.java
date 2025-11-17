package com.ptithcm.movie.external.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MultiSearchItemDto(
        int id,
        @JsonProperty("media_type") String mediaType, // "movie" hoặc "tv"

        // Trường của Movie
        String title,
        @JsonProperty("release_date") String releaseDate,

        // Trường của TV Show (sẽ là 'null' nếu là movie)
        String name,
        @JsonProperty("first_air_date") String firstAirDate,

        // Trường chung
        String overview,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("vote_average") double voteAverage
) {
    // Helper để React lấy tên hiển thị (cho TV hoặc Movie)
    public String getNormalizedTitle() {
        return "movie".equals(mediaType) ? title : name;
    }
}
