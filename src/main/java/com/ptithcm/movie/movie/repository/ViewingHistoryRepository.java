package com.ptithcm.movie.movie.repository;

import com.ptithcm.movie.movie.entity.ViewingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ViewingHistoryRepository extends JpaRepository<ViewingHistory, UUID> {
    boolean existsByMovieId(UUID movieId);
}