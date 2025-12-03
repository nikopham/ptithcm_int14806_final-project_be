package com.ptithcm.movie.movie.repository;

import com.ptithcm.movie.movie.entity.MovieLike;
import com.ptithcm.movie.movie.entity.MovieLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieLikeRepository extends JpaRepository<MovieLike, MovieLikeId> {

    boolean existsById(MovieLikeId id);

    long countByMovieId(java.util.UUID movieId);
}