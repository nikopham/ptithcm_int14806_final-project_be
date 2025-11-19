package com.ptithcm.movie.external.tmdb.dto;

import java.util.List;

public record TmdbGenreListDto(
        List<GenreDto> genres
) {}