package com.ptithcm.movie.movie.controller;

import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.movie.dto.request.ReviewRequest;
import com.ptithcm.movie.movie.dto.request.ReviewSearchRequest;
import com.ptithcm.movie.movie.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/search")
    public ResponseEntity<ServiceResult> searchReviews(
            @ModelAttribute ReviewSearchRequest request,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.searchReviews(request, pageable));
    }

    @PostMapping("/add")
    public ResponseEntity<ServiceResult> createReview(
            @RequestBody @Valid ReviewRequest request
    ) {
        return ResponseEntity.ok(reviewService.createReview(request));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ServiceResult> updateReview(
            @PathVariable UUID id,
            @RequestBody @Valid ReviewRequest request
    ) {
        return ResponseEntity.ok(reviewService.updateReview(id, request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ServiceResult> deleteReview(@PathVariable UUID id) {
        return ResponseEntity.ok(reviewService.deleteReview(id));
    }

    @PatchMapping("/{id}/toggle-hidden")
    public ResponseEntity<ServiceResult> toggleHidden(@PathVariable UUID id) {
        return ResponseEntity.ok(reviewService.toggleHidden(id));
    }
}
