package com.ptithcm.movie.movie.repository;

import com.ptithcm.movie.movie.dto.GenreItemDto;
import com.ptithcm.movie.movie.entity.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genre, Integer> {
    // Tự động tìm theo tmdb_id
    Optional<Genre> findByTmdbId(Integer tmdbId);

    @Query("SELECT new com.ptithcm.movie.movie.dto.GenreItemDto(g.id, g.name, g.tmdbId, COUNT(m)) " +
            "FROM Genre g " +
            "LEFT JOIN g.movies m " +
            "WHERE (:query IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "GROUP BY g.id")
    Page<GenreItemDto> searchGenresWithCount(@Param("query") String query, Pageable pageable);

    // Kiểm tra xem tên đã tồn tại chưa (không phân biệt hoa thường)
    boolean existsByNameIgnoreCase(String name);

    // Kiểm tra xem TMDb ID đã tồn tại chưa
    boolean existsByTmdbId(Integer tmdbId);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);

    boolean existsByTmdbIdAndIdNot(Integer tmdbId, Integer id);
}
