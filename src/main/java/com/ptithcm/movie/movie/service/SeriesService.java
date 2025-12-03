package com.ptithcm.movie.movie.service;

import com.ptithcm.movie.common.constant.ErrorCode;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.movie.dto.request.EpisodeCreateRequest;
import com.ptithcm.movie.movie.dto.request.EpisodeUpdateRequest;
import com.ptithcm.movie.movie.dto.request.SeasonCreateRequest;
import com.ptithcm.movie.movie.dto.request.SeasonUpdateRequest;
import com.ptithcm.movie.movie.entity.Episode;
import com.ptithcm.movie.movie.entity.Movie;
import com.ptithcm.movie.movie.entity.Season;
import com.ptithcm.movie.movie.repository.EpisodeRepository;
import com.ptithcm.movie.movie.repository.MovieRepository;
import com.ptithcm.movie.movie.repository.SeasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeriesService {

    private final MovieRepository movieRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;

    @Transactional
    public ServiceResult addSeason(UUID movieId, SeasonCreateRequest request) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        if (!movie.isSeries()) {
            return ServiceResult.Failure().message("This movie is not a TV Series");
        }

        if (seasonRepository.existsByMovieIdAndSeasonNumber(movieId, request.getSeasonNumber())) {
            return ServiceResult.Failure().message("Season number " + request.getSeasonNumber() + " already exists");
        }

        Season season = Season.builder()
                .movie(movie)
                .seasonNumber(request.getSeasonNumber())
                .title(request.getTitle())
                .build();

        Season savedSeason = seasonRepository.save(season);
        return ServiceResult.Success().code(ErrorCode.SUCCESS).message("Season created successfully");
    }

    @Transactional
    public ServiceResult addEpisode(UUID seasonId, EpisodeCreateRequest request) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new RuntimeException("Season not found"));

        if (episodeRepository.existsBySeasonIdAndEpisodeNumber(seasonId, request.getEpisodeNumber())) {
            return ServiceResult.Failure().message("Episode number " + request.getEpisodeNumber() + " already exists in this season");
        }

        Episode episode = Episode.builder()
                .season(season)
                .episodeNumber(request.getEpisodeNumber())
                .title(request.getTitle())
                .durationMin(request.getDurationMin())
                .synopsis(request.getSynopsis())
                .airDate(request.getAirDate())
                .build();

        Episode savedEpisode = episodeRepository.save(episode);
        return ServiceResult.Success().code(ErrorCode.SUCCESS).message("Episode created successfully");
    }

    // --- UPDATE SEASON ---
    @Transactional
    public ServiceResult updateSeason(UUID id, SeasonUpdateRequest request) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Season not found"));

        // Check trùng season number (nếu có thay đổi)
        if (request.getSeasonNumber() != null && !request.getSeasonNumber().equals(season.getSeasonNumber())) {
            boolean exists = seasonRepository.existsByMovieIdAndSeasonNumberAndIdNot(
                    season.getMovie().getId(),
                    request.getSeasonNumber(),
                    id
            );
            if (exists) {
                return ServiceResult.Failure().message("Season number already exists");
            }
            season.setSeasonNumber(request.getSeasonNumber());
        }

        if (request.getTitle() != null) season.setTitle(request.getTitle());
        Season saved = seasonRepository.save(season);
        return ServiceResult.Success().code(ErrorCode.SUCCESS).message("Season updated successfully");
    }

    // --- UPDATE EPISODE ---
    @Transactional
    public ServiceResult updateEpisode(UUID id, EpisodeUpdateRequest request) {
        Episode episode = episodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Episode not found"));

        // Check trùng episode number
        if (request.getEpisodeNumber() != null && !request.getEpisodeNumber().equals(episode.getEpisodeNumber())) {
            boolean exists = episodeRepository.existsBySeasonIdAndEpisodeNumberAndIdNot(
                    episode.getSeason().getId(),
                    request.getEpisodeNumber(),
                    id
            );
            if (exists) {
                return ServiceResult.Failure().message("Episode number already exists in this season");
            }
            episode.setEpisodeNumber(request.getEpisodeNumber());
        }

        if (request.getTitle() != null) episode.setTitle(request.getTitle());
        if (request.getDurationMin() != null) episode.setDurationMin(request.getDurationMin());
        if (request.getSynopsis() != null) episode.setSynopsis(request.getSynopsis());
        if (request.getAirDate() != null) episode.setAirDate(request.getAirDate());
        Episode saved = episodeRepository.save(episode);
        return ServiceResult.Success().code(ErrorCode.SUCCESS).message("Episode updated successfully");
    }
}