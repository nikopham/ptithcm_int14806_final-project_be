package com.ptithcm.movie.comment.repository;

import com.ptithcm.movie.comment.entity.MovieComment;
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
public interface MovieCommentRepository extends JpaRepository<MovieComment, UUID>, JpaSpecificationExecutor<MovieComment> {
    boolean existsByMovieId(UUID movieId);

    @Query("SELECT c.user.id, COUNT(DISTINCT c.movie.id) " +
            "FROM MovieComment c " +
            "WHERE c.user.id IN :userIds " +
            "GROUP BY c.user.id")
    List<Object[]> countMoviesCommentedByUserIds(@Param("userIds") List<UUID> userIds);

    @Query("SELECT c.user.id, COUNT(c) " +
            "FROM MovieComment c " +
            "WHERE c.user.id IN :userIds " +
            "GROUP BY c.user.id")
    List<Object[]> countCommentsByUserIds(@Param("userIds") List<UUID> userIds);

    boolean existsByUser_Id(UUID userId);

    @Query("SELECT c FROM MovieComment c " +
            "JOIN FETCH c.user u " +
            "LEFT JOIN FETCH u.role " +
            "WHERE c.movie.id = :movieId " +

            "AND c.isHidden = false " +
            "ORDER BY c.createdAt DESC")
    Page<MovieComment> findRootCommentsByMovieId(@Param("movieId") UUID movieId, Pageable pageable);


    @Query("SELECT c.parent.id, COUNT(c) " +
            "FROM MovieComment c " +
            "WHERE c.parent.id IN :parentIds " +
            "AND c.isHidden = false " +
            "GROUP BY c.parent.id")
    List<Object[]> countRepliesByParentIds(@Param("parentIds") List<UUID> parentIds);
}
