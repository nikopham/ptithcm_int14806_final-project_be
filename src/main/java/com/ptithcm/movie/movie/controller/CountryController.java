package com.ptithcm.movie.movie.controller;

import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.movie.service.CountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/countries")
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countryService;

    @GetMapping("/get-all")
    public ResponseEntity<ServiceResult> getAllCountries() {
        return ResponseEntity.ok(countryService.getAllCountries());
    }
}
