package com.ptithcm.movie.movie.repository;

import com.ptithcm.movie.common.constant.MovieStatus;
import com.ptithcm.movie.movie.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID>, JpaSpecificationExecutor<Movie> {
    @Query("SELECT COALESCE(SUM(m.viewCount), 0) FROM Movie m")
    Long sumTotalViews();

    long count();


    @Modifying
    @Query("UPDATE Movie m SET m.viewCount = COALESCE(m.viewCount, 0) + 1 WHERE m.id = :id")
    void incrementViewCount(@Param("id") UUID id);

    @Query("SELECT m.id FROM Movie m")
    List<UUID> findAllIds();

    List<Movie> findTop10ByOrderByViewCountDesc();



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

    Optional<Movie> findMovieByTitleIgnoreCase(String title);

    @Query("SELECT DISTINCT m FROM Movie m " +
            "LEFT JOIN m.actors a " +
            "LEFT JOIN m.directors d " +
            "WHERE (a.id = :personId OR d.id = :personId) " +
            "AND m.status = 'PUBLISHED'")
    Page<Movie> findByPersonId(@Param("personId") UUID personId, Pageable pageable);

    @Query("SELECT DISTINCT YEAR(m.releaseDate) " +
            "FROM Movie m " +
            "WHERE m.status = 'PUBLISHED' " +
            "AND m.releaseDate IS NOT NULL " +
            "ORDER BY YEAR(m.releaseDate) DESC")
    List<Integer> findDistinctReleaseYears();
}