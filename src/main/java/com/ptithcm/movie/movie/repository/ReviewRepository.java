package com.ptithcm.movie.movie.repository;

import com.ptithcm.movie.movie.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID>, JpaSpecificationExecutor<Review> {
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.movie.id = :movieId")
    Double getAverageRatingByMovieId(@Param("movieId") UUID movieId);

    long countByMovieId(UUID movieId);

    List<Review> findTop5ByMovieIdOrderByCreatedAtDesc(UUID movieId);

    boolean existsByMovieId(UUID movieId);

    @Query("SELECT r.user.id, COUNT(DISTINCT r.movie.id) " +
            "FROM Review r " +
            "WHERE r.user.id IN :userIds " +
            "GROUP BY r.user.id")
    List<Object[]> countMoviesReviewedByUserIds(@Param("userIds") List<UUID> userIds);

    @Query(value = "SELECT r FROM Review r " +
            "JOIN FETCH r.user u " +
            "WHERE r.movie.id = :movieId",
            countQuery = "SELECT COUNT(r) FROM Review r WHERE r.movie.id = :movieId")
    Page<Review> findAllByMovieId(@Param("movieId") UUID movieId, Pageable pageable);

    boolean existsByUserIdAndMovieId(UUID userId, UUID movieId);
}
