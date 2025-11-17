package com.ptithcm.movie.movie.controller;

import com.ptithcm.movie.common.constant.PersonJob;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.external.tmdb.TMDbService;
import com.ptithcm.movie.external.tmdb.dto.*;
import com.ptithcm.movie.movie.entity.Country;
import com.ptithcm.movie.movie.entity.Genre;
import com.ptithcm.movie.movie.repository.CountryRepository;
import com.ptithcm.movie.movie.repository.GenreRepository;
import com.ptithcm.movie.movie.service.CountryService;
import com.ptithcm.movie.movie.service.GenreService;
import com.ptithcm.movie.movie.service.MovieService;
import com.ptithcm.movie.movie.service.PeopleService;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/movies") // Endpoint gốc
public class MovieController {

    private final TMDbService tmdbService;
    private final MovieService movieService;
    private final GenreService genreService;
    private final CountryService countryService;
    private final PeopleService peopleService;

    /**
     * API 1a: /api/movies/search/movie?query=...
     */
    @GetMapping("/search/movie")
    public ServiceResult searchMovies(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page
    ) {
        try {
            MovieSearchResultDto data = tmdbService.searchMovies(query, page);
            return ServiceResult.Success().data(data);
        } catch (Exception e) {
            return ServiceResult.Failure().message(e.getMessage());
        }
    }

    /**
     * API 1b: /api/movies/search/tv?query=...
     */
    @GetMapping("/search/tv")
    public ServiceResult searchTvShows(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page
    ) {
        try {
            TvSearchResultDto data = tmdbService.searchTvShows(query, page);
            return ServiceResult.Success().data(data);
        } catch (Exception e) {
            return ServiceResult.Failure().message(e.getMessage());
        }
    }

    /**
     * API 2a: /api/movies/details/movie/{movieId}
     */
    @GetMapping("/details/movie/{movieId}")
    public ServiceResult getMovieDetails(@PathVariable int movieId) {
        try {
            MovieDetailDto data = tmdbService.getMovieFullDetails(movieId);
            return ServiceResult.Success().data(data);
        } catch (Exception e) {
            return ServiceResult.Failure().message(e.getMessage());
        }
    }

    /**
     * API 2b: /api/movies/details/tv/{tvId}
     */
    @GetMapping("/details/tv/{tvId}")
    public ServiceResult getTvShowDetails(@PathVariable int tvId) {
        try {
            TvDetailDto data = tmdbService.getTvShowFullDetails(tvId);
            return ServiceResult.Success().data(data);
        } catch (Exception e) {
            return ServiceResult.Failure().message(e.getMessage());
        }
    }

    /**
     * API 3: /api/movies/details/tv/{tvId}/season/{seasonNumber}
     */
    @GetMapping("/details/tv/{tvId}/season/{seasonNumber}")
    public ServiceResult getTvSeasonDetails(
            @PathVariable int tvId,
            @PathVariable int seasonNumber
    ) {
        try {
            TvSeasonDetailDto data = tmdbService.getTvSeasonDetails(tvId, seasonNumber);
            return ServiceResult.Success().data(data);
        } catch (Exception e) {
            return ServiceResult.Failure().message(e.getMessage());
        }
    }
    // --- movie internal api ---

    /**
     * API lấy TẤT CẢ genres từ DB
     */
    @GetMapping("/genres")
    public ServiceResult getAllGenres() {
        // 4. Gọi Service thay vì Repository
        return genreService.getAllGenres();
    }

    /**
     * API lấy TẤT CẢ countries từ DB
     */
    @GetMapping("/countries")
    public ServiceResult getAllCountries() {
        // 4. Gọi Service thay vì Repository
        return countryService.getAllCountries();
    }


    @GetMapping("/peoples/search")
    public ServiceResult searchPeople(
            @RequestParam String query,
            @RequestParam PersonJob job // Spring tự động chuyển đổi string "ACTOR" thành Enum
    ) {
        return peopleService.searchPeople(query, job);
    }
}