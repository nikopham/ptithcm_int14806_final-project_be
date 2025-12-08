package com.ptithcm.movie.movie.repository;

import com.ptithcm.movie.movie.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CountryRepository extends JpaRepository<Country, Integer> {
    @Query("SELECT DISTINCT c FROM Movie m " +
            "JOIN m.countries c " +
            "WHERE m.status = 'PUBLISHED' " +
            "ORDER BY c.name ASC")
    List<Country> findAllWithPublishedMovies();
}