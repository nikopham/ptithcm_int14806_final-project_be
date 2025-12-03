package com.ptithcm.movie.movie.repository;

import com.ptithcm.movie.movie.entity.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Integer>, JpaSpecificationExecutor<Genre> {
    @Query("SELECT g.id, COUNT(m.id) FROM Movie m JOIN m.genres g WHERE g.id IN :ids GROUP BY g.id")
    List<Object[]> countMoviesByGenreIds(@Param("ids") List<Integer> ids);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);

    @Query("SELECT g FROM Genre g " +
            "JOIN g.movies m " +
            "WHERE m.status = 'PUBLISHED' " +
            "AND (:isSeries IS NULL OR m.isSeries = :isSeries) " +
            "GROUP BY g.id " +
            "ORDER BY MAX(m.createdAt) DESC")
    List<Genre> findTopGenresByLatestMovies(
            @Param("isSeries") Boolean isSeries,
            Pageable pageable
    );

    @Query("SELECT COUNT(m) FROM Movie m JOIN m.genres g WHERE g.id = :genreId")
    long countMoviesByGenreId(@Param("genreId") Integer genreId);

    @Modifying
    @Query(value = "DELETE FROM movie_genres WHERE genre_id = :genreId", nativeQuery = true)
    void deleteMovieGenreRelations(@Param("genreId") Integer genreId);
}
