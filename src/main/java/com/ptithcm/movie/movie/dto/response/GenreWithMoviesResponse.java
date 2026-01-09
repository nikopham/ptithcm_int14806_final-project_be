package com.ptithcm.movie.movie.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GenreWithMoviesResponse {
    private Integer genreId;
    private String genreName;
    private List<MovieShortResponse> movies;
}