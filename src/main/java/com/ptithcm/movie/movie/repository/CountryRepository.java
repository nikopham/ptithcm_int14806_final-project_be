package com.ptithcm.movie.movie.repository;

import com.ptithcm.movie.movie.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CountryRepository extends JpaRepository<Country, Integer> {
    // Tự động tìm theo iso_code
    Optional<Country> findByIsoCode(String isoCode);
}