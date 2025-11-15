package com.ptithcm.movie.movie.tmdb;

import com.ptithcm.movie.movie.tmdb.dto.TmdbMovieBrief;
import com.ptithcm.movie.movie.tmdb.dto.TmdbMovieDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TmdbService {

    private final TmdbClient tmdb;

    @Cacheable(value = "tmdb_movie", key = "#id")
    public TmdbMovieDetail getDetail(int id) {
        return tmdb.getMovie(id).block();
    }

    public List<TmdbMovieBrief> search(String q, int page) {
        return tmdb.searchMovies(q, page).collectList().block();
    }
}

