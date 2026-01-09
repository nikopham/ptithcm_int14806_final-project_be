package com.ptithcm.movie.movie.controller;

import com.ptithcm.movie.movie.dto.response.DashboardOverviewResponse;
import com.ptithcm.movie.movie.dto.response.GenreStatResponse;
import com.ptithcm.movie.movie.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewResponse> getOverview() {
        return ResponseEntity.ok(dashboardService.getOverview());
    }

    @GetMapping("/views-chart")
    public ResponseEntity<List<Long>> getViewsChart(@RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(dashboardService.getMonthlyViews(year));
    }

    @GetMapping("/top-genres")
    public ResponseEntity<List<GenreStatResponse>> getTopGenres() {
        return ResponseEntity.ok(dashboardService.getTopGenres());
    }
}
