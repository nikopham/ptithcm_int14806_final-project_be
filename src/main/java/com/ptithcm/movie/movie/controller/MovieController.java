package com.ptithcm.movie.movie.controller;

import com.ptithcm.movie.auth.security.UserPrincipal;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.external.cloudflare.CloudflareService;
import com.ptithcm.movie.external.smart.RecommendationService;
import com.ptithcm.movie.movie.dto.request.MovieCreateRequest;
import com.ptithcm.movie.movie.dto.request.MovieSearchRequest;
import com.ptithcm.movie.movie.dto.request.MovieUpdateRequest;
import com.ptithcm.movie.movie.dto.request.WatchProgressRequest;
import com.ptithcm.movie.movie.dto.response.MovieShortResponse;
import com.ptithcm.movie.movie.service.MovieService;
import com.ptithcm.movie.movie.service.ReviewService;
import com.ptithcm.movie.user.entity.User;
import com.ptithcm.movie.user.repository.UserRepository;
import com.ptithcm.movie.utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final ReviewService reviewService;
    private final CloudflareService cloudflareService;
    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> recordView(@PathVariable UUID id) {
        movieService.increaseViewCount(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/years")
    public ResponseEntity<ServiceResult> getReleaseYears() {
        return ResponseEntity.ok(movieService.getReleaseYears());
    }

    @GetMapping("/search")
    public ResponseEntity<ServiceResult> searchMovies(
            @ModelAttribute MovieSearchRequest request,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        ServiceResult result = movieService.searchMovies(request, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search-liked")
    public ResponseEntity<ServiceResult> getLikedMovies(
            @ModelAttribute MovieSearchRequest request,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(movieService.searchMovieUserLike(request, pageable));
    }

    @PostMapping(path = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServiceResult> createMovie(
            @ModelAttribute MovieCreateRequest request
    ) {
        ServiceResult result = movieService.createMovie(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping(path = "/top-10")
    public ResponseEntity<ServiceResult> getTop10Movie(
            @RequestParam(required = false) Boolean isSeries
    ) {
        ServiceResult result = movieService.getMostViewedMovies(isSeries);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/info")
    public ResponseEntity<ServiceResult> getMovieInfo(@PathVariable UUID id) {
        return ResponseEntity.ok(movieService.getMovieInfo(id));
    }

    @PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServiceResult> updateMovie(
            @PathVariable UUID id,
            @ModelAttribute MovieUpdateRequest request
    ) {
        return ResponseEntity.ok(movieService.updateMovie(id, request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ServiceResult> deleteMovie(@PathVariable UUID id) {
        return ResponseEntity.ok(movieService.deleteMovie(id));
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<ServiceResult> getMovieDetail(@PathVariable UUID id ,
                                                        HttpServletRequest request) {
        String clientIp = IpUtils.getClientIp(request);
        return ResponseEntity.ok(movieService.getMovieDetail(id, clientIp));
    }

    @GetMapping("/detail/{id}/reviews")
    public ResponseEntity<ServiceResult> getMovieReviews(
            @PathVariable UUID id,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getReviewsByMovie(id, pageable));
    }

    @PostMapping("/like/{id}")
    public ResponseEntity<ServiceResult> toggleLikeMovie(@PathVariable UUID id) {
        return ResponseEntity.ok(movieService.toggleLikeMovie(id));
    }

    @GetMapping("/video-status/{videoUid}")
    public ResponseEntity<ServiceResult> getVideoStatus(@PathVariable String videoUid) {
        return ResponseEntity.ok(movieService.checkAndSyncVideoStatus(videoUid));
    }

    @PostMapping("/progress")
    public ResponseEntity<Void> saveProgress(@RequestBody WatchProgressRequest request) {
        movieService.saveProgress(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/watched")
    public ResponseEntity<ServiceResult> getWatchedMovies(
            @ModelAttribute MovieSearchRequest request,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(movieService.searchWatchedMovies(request, pageable));
    }

    @GetMapping("/recommend-for-you")
    public ResponseEntity<List<MovieShortResponse>> getRecommendations() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.ok(List.of());
        }

        List<MovieShortResponse> list = recommendationService.getRecommendations(currentUser.getId());

        return ResponseEntity.ok(list);
    }

    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        UUID userId = null;

        if (principal instanceof UserPrincipal userPrincipal) {
            User u = userPrincipal.getUser();
            userId = u.getId();
        }

        if (userId != null) {
            return userRepository.findById(userId).orElse(null);
        }
        return null;
    }
}
