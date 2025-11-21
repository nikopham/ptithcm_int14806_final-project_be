package com.ptithcm.movie.movie.repository;

import com.ptithcm.movie.common.constant.MovieStatus;
import com.ptithcm.movie.movie.entity.Movie;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID>,
        JpaSpecificationExecutor<Movie> {

}
