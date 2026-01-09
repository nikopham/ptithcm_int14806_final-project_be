package com.ptithcm.movie.movie.repository;

import com.ptithcm.movie.movie.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeasonRepository extends JpaRepository<Season, UUID> {
    boolean existsByMovieIdAndSeasonNumber(UUID movieId, Integer seasonNumber);
    @Query("SELECT s FROM Season s " +
            "LEFT JOIN FETCH s.episodes e " +
            "WHERE s.movie.id = :movieId " +
            "ORDER BY s.seasonNumber ASC, e.episodeNumber ASC")
    List<Season> findAllByMovieIdWithEpisodes(@Param("movieId") UUID movieId);

    boolean existsByMovieIdAndSeasonNumberAndIdNot(UUID movieId, Integer seasonNumber, UUID id);

    boolean existsById(UUID id);

    @Query("select s from Season s left join fetch s.episodes where s.id = :id")
    Optional<Season> findByIdWithEpisodes(@Param("id") UUID id);
}
