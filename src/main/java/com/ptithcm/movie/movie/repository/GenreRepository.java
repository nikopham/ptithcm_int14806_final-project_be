package com.ptithcm.movie.movie.repository;

import com.ptithcm.movie.movie.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genre, Integer> {
    // Tự động tìm theo tmdb_id
    Optional<Genre> findByTmdbId(Integer tmdbId);
}
