package com.ptithcm.movie.movie.controller;

import com.ptithcm.movie.common.constant.ErrorCode;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.movie.dto.request.EpisodeCreateRequest;
import com.ptithcm.movie.movie.dto.request.EpisodeUpdateRequest;
import com.ptithcm.movie.movie.dto.request.SeasonCreateRequest;
import com.ptithcm.movie.movie.dto.request.SeasonUpdateRequest;
import com.ptithcm.movie.movie.service.SeriesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SeriesController {

    private final SeriesService seriesService;

    @PostMapping("/movies/add/{movieId}/seasons")
    public ResponseEntity<ServiceResult> addSeason(
            @PathVariable UUID movieId,
            @RequestBody @Valid SeasonCreateRequest request
    ) {
        return ResponseEntity.ok(seriesService.addSeason(movieId, request));
    }

    @PostMapping("/seasons/add/{seasonId}/episodes")
    public ResponseEntity<ServiceResult> addEpisode(
            @PathVariable UUID seasonId,
            @RequestBody @Valid EpisodeCreateRequest request
    ) {
        return ResponseEntity.ok(seriesService.addEpisode(seasonId, request));
    }

    // Update Season
    @PutMapping("/seasons/update/{id}")
    public ResponseEntity<ServiceResult> updateSeason(
            @PathVariable UUID id,
            @RequestBody @Valid SeasonUpdateRequest request
    ) {
        System.out.println(id);
        return ResponseEntity.ok(seriesService.updateSeason(id, request));
    }

    // Update Episode
    @PutMapping("/episodes/update/{id}")
    public ResponseEntity<ServiceResult> updateEpisode(
            @PathVariable UUID id,
            @RequestBody @Valid EpisodeUpdateRequest request
    ) {
        return ResponseEntity.ok(seriesService.updateEpisode(id, request));
    }

    @GetMapping("/seasons/{seasonId}/episodes")
    public ResponseEntity<ServiceResult> getEpisodesBySeason(@PathVariable UUID seasonId) {
        ServiceResult result = seriesService.getEpisodesBySeasonId(seasonId);
        if (result.getCode() == ErrorCode.FAILED) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        return ResponseEntity.ok(result);
    }
}