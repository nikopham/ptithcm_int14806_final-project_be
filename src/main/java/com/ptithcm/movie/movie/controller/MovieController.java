package com.ptithcm.movie.movie.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptithcm.movie.common.constant.MovieStatus;
import com.ptithcm.movie.common.constant.PersonJob;
import com.ptithcm.movie.common.dto.PagedResponseDto;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.external.tmdb.TMDbService;
import com.ptithcm.movie.external.tmdb.dto.*;
import com.ptithcm.movie.movie.dto.MovieItemDto;
import com.ptithcm.movie.movie.dto.MovieRequestDto;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/movies") // Endpoint gốc
public class MovieController {

    private final TMDbService tmdbService;
    private final MovieService movieService;
    private final GenreService genreService;
    private final CountryService countryService;
    private final PeopleService peopleService;
    private final ObjectMapper objectMapper;

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

    @PostMapping(path ="/add",consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ServiceResult addMovie(
            @RequestPart("dto") String movieRequestString,
            @RequestPart(value = "posterFile", required = false) MultipartFile posterFile,
            @RequestPart(value = "backdropFile", required = false) MultipartFile backdropFile,
            @AuthenticationPrincipal UserDetails userDetails
    ) throws Exception {
        MovieRequestDto dto = objectMapper.readValue(movieRequestString, MovieRequestDto.class);
        return movieService.createMovie(dto, posterFile, backdropFile, userDetails);
    }

    @GetMapping("/list")
    public ServiceResult getPaginatedMovies(
            // Tham số cho Tìm kiếm & Lọc
            @RequestParam(required = false) String query,
            @RequestParam(required = false) MovieStatus status, // Spring tự đổi "PUBLISHED" -> Enum
            @RequestParam(required = false) Boolean isSeries,   // "true" -> true

            // Tham số cho Phân trang
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            PagedResponseDto<MovieItemDto> result = movieService.getMovies(query, status, isSeries, page, size);
            return ServiceResult.Success()
                    .message("Movies fetched successfully")
                    .data(result);
        } catch (Exception e) {
            return ServiceResult.Failure()
                    .message("Failed to fetch movies: " + e.getMessage());
        }
    }

    @GetMapping("/detail/{id}")
    public ServiceResult getMovieById(@PathVariable UUID id) {
        return movieService.getMovieById(id);
    }
    /**
     * (MỚI) API Cập nhật Movie (Core)
     * (Frontend gửi multipart/form-data y hệt như 'addMovie')
     */
    @PutMapping(value = "/update/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ServiceResult updateMovie(
            @PathVariable UUID id,
            @RequestPart("dto") String movieRequestString,
            @RequestPart(value = "posterFile", required = false) MultipartFile posterFile,
            @RequestPart(value = "backdropFile", required = false) MultipartFile backdropFile,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            MovieRequestDto dto = objectMapper.readValue(movieRequestString, MovieRequestDto.class);
            return movieService.updateMovie(id, dto, posterFile, backdropFile, userDetails);
        } catch (Exception e) {
            return ServiceResult.Failure()
                    .message("Failed to process update request: " + e.getMessage());
        }
    }
}