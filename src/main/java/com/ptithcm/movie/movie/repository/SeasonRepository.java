package com.ptithcm.movie.movie.repository;

import com.ptithcm.movie.movie.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SeasonRepository extends JpaRepository<Season, UUID> {
    @Query("SELECT s FROM Season s LEFT JOIN FETCH s.episodes WHERE s.movie.id = :movieId")
    List<Season> findByMovieIdWithEpisodes(@Param("movieId") UUID movieId);
}
