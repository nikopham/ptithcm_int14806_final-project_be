package com.ptithcm.movie.movie.repository;

import com.ptithcm.movie.common.constant.MovieStatus;
import com.ptithcm.movie.movie.entity.Movie;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID>, JpaSpecificationExecutor<Movie> {
    @Query("SELECT m FROM Movie m " +
            "JOIN m.genres g " +
            "WHERE g.id = :genreId " +
            "AND m.status = 'PUBLISHED' " +
            "AND (:isSeries IS NULL OR m.isSeries = :isSeries) " +
            "ORDER BY m.createdAt DESC")
    List<Movie> findLatestByGenre(
            @Param("genreId") Integer genreId,
            @Param("isSeries") Boolean isSeries,
            Pageable pageable
    );

    @Query("SELECT m FROM Movie m " +
            "WHERE m.status = 'PUBLISHED' " +
            "AND (:isSeries IS NULL OR m.isSeries = :isSeries) " +
            "ORDER BY m.viewCount DESC")
    List<Movie> findMostViewed(
            @Param("isSeries") Boolean isSeries,
            Pageable pageable
    );

    @Query("SELECT m.createdBy.id, COUNT(m) " +
            "FROM Movie m " +
            "WHERE m.createdBy.id IN :userIds " +
            "GROUP BY m.createdBy.id")
    List<Object[]> countMoviesCreatedByUserIds(@Param("userIds") List<UUID> userIds);

    @Query("SELECT m.updatedBy.id, COUNT(m) " +
            "FROM Movie m " +
            "WHERE m.updatedBy.id IN :userIds " +
            "GROUP BY m.updatedBy.id")
    List<Object[]> countMoviesUpdatedByUserIds(@Param("userIds") List<UUID> userIds);

    boolean existsByCreatedBy_Id(UUID userId);

    boolean existsByUpdatedBy_Id(UUID userId);

    Optional<Movie> findByVideoUrlContaining(String videoUid);
}