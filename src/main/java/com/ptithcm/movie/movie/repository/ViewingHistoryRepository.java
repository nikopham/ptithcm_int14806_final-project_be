package com.ptithcm.movie.movie.repository;

import com.ptithcm.movie.movie.dto.response.MonthlyViewStat;
import com.ptithcm.movie.movie.entity.ViewingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ViewingHistoryRepository extends JpaRepository<ViewingHistory, UUID> {
    @Query("SELECT new com.ptithcm.movie.movie.dto.response.MonthlyViewStat(CAST(EXTRACT(MONTH FROM v.lastWatchedAt) AS int), COUNT(v)) " +
            "FROM ViewingHistory v " +
            "WHERE CAST(EXTRACT(YEAR FROM v.lastWatchedAt) AS int) = :year " +
            "GROUP BY EXTRACT(MONTH FROM v.lastWatchedAt) " +
            "ORDER BY EXTRACT(MONTH FROM v.lastWatchedAt) ASC")
    List<MonthlyViewStat> countViewsByMonth(@Param("year") int year);

    boolean existsByMovieId(UUID movieId);
    Optional<ViewingHistory> findByUserIdAndMovieIdAndEpisodeId(UUID userId, UUID movieId, UUID episodeId);
    List<ViewingHistory> findByUserIdAndMovieId(UUID userId, UUID movieId);
    Optional<ViewingHistory> findTopByUserIdOrderByLastWatchedAtDesc(UUID userId);

    @Query(value = """
        SELECT 
            CAST(user_id AS VARCHAR) as userId, 
            CAST(movie_id AS VARCHAR) as movieId, 
            MAX(rating) as finalRating 
        FROM (
            -- 1. DỮ LIỆU TỪ LỊCH SỬ XEM (Implicit)
            SELECT 
                user_id, movie_id,
                (CASE 
                    WHEN (total_seconds > 0 AND (CAST(accumulated_seconds AS FLOAT) / total_seconds) >= 0.8) THEN 4.0
                    WHEN (total_seconds > 0 AND (CAST(accumulated_seconds AS FLOAT) / total_seconds) >= 0.5) THEN 3.0
                    WHEN (total_seconds > 0 AND (CAST(accumulated_seconds AS FLOAT) / total_seconds) >= 0.1) THEN 2.0
                    ELSE 0.0 
                END) as rating
            FROM viewing_history
            WHERE accumulated_seconds > 0

            UNION ALL

            -- 2. DỮ LIỆU TỪ LIKE (Explicit) -> 5 ĐIỂM
            SELECT user_id, movie_id, 5.0 as rating FROM movie_likes
        ) as combined
        GROUP BY user_id, movie_id
        HAVING MAX(rating) > 0
    """, nativeQuery = true)
    List<Map<String, Object>> getTrainingData();
}