package com.ptithcm.movie.movie.controller;

import com.ptithcm.movie.common.dto.PagedResponseDto;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.external.tmdb.TMDbService;
import com.ptithcm.movie.external.tmdb.dto.GenreDto;
import com.ptithcm.movie.movie.dto.CreateGenreRequest;
import com.ptithcm.movie.movie.dto.GenreItemDto;
import com.ptithcm.movie.movie.dto.UpdateGenreRequest;
import com.ptithcm.movie.movie.repository.GenreRepository;
import com.ptithcm.movie.movie.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/genres")
@RequiredArgsConstructor
public class GenreController {
    private final GenreService genreService;
    private final TMDbService  tmdbService;

    @GetMapping("/list")
    public ServiceResult searchGenres(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return genreService.getGenresPaginated(query, page, size);
    }

    @GetMapping("/tmdb/genres")
    public ServiceResult searchTmdbGenres(
            @RequestParam(required = false, defaultValue = "") String query
    ) {
        try {
            List<GenreDto> genres = tmdbService.searchGenresOnTmdb(query);
            return ServiceResult.Success()
                    .message("Found " + genres.size() + " genres from TMDb.")
                    .data(genres);
        } catch (Exception e) {
            return ServiceResult.Failure()
                    .message("Failed to fetch genres from TMDb: " + e.getMessage());
        }
    }

    @PostMapping("/add")
    public ServiceResult createGenre(@RequestBody CreateGenreRequest request) {
        return genreService.createGenre(request);
    }

    @PutMapping("/update/{id}")
    public ServiceResult updateGenre(
            @PathVariable Integer id,
            @RequestBody UpdateGenreRequest request
    ) {
        return genreService.updateGenre(id, request);
    }
}
