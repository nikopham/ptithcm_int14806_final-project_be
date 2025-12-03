package com.ptithcm.movie.movie.controller;

import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.movie.dto.request.GenreRequest;
import com.ptithcm.movie.movie.dto.request.GenreSearchRequest;
import com.ptithcm.movie.movie.service.GenreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/genres")
@RequiredArgsConstructor
public class GenreController {

    private final GenreService genreService;

    @GetMapping("/search")
    public ResponseEntity<ServiceResult> searchGenres(
            @ModelAttribute GenreSearchRequest request,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        ServiceResult result = genreService.searchGenres(request, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/get-all")
    public ResponseEntity<ServiceResult> getAllGenres() {
        return ResponseEntity.ok(genreService.getAllGenres());
    }

    @PostMapping("/add")
    public ResponseEntity<ServiceResult> createGenre(@RequestBody @Valid GenreRequest request) {
        return ResponseEntity.ok(genreService.createGenre(request));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ServiceResult> updateGenre(
            @PathVariable Integer id,
            @RequestBody @Valid GenreRequest request
    ) {
        return ResponseEntity.ok(genreService.updateGenre(id, request));
    }

    @GetMapping("/featured")
    public ResponseEntity<ServiceResult> getFeaturedGenres(
            @RequestParam(required = false) Boolean isSeries
    ) {
        return ResponseEntity.ok(genreService.getFeaturedGenresWithMovies(isSeries));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ServiceResult> deleteGenre(@PathVariable Integer id) {
        return ResponseEntity.ok(genreService.deleteGenre(id));
    }
}