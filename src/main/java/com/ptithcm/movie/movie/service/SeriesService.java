package com.ptithcm.movie.movie.service;

import com.ptithcm.movie.common.constant.ErrorCode;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.external.cloudflare.CloudflareStreamService;
import com.ptithcm.movie.movie.dto.request.EpisodeCreateRequest;
import com.ptithcm.movie.movie.dto.request.EpisodeUpdateRequest;
import com.ptithcm.movie.movie.dto.request.SeasonCreateRequest;
import com.ptithcm.movie.movie.dto.request.SeasonUpdateRequest;
import com.ptithcm.movie.movie.dto.response.CreateSeasonDto;
import com.ptithcm.movie.movie.dto.response.MovieInfoResponse;
import com.ptithcm.movie.movie.entity.Episode;
import com.ptithcm.movie.movie.entity.Movie;
import com.ptithcm.movie.movie.entity.Season;
import com.ptithcm.movie.movie.repository.EpisodeRepository;
import com.ptithcm.movie.movie.repository.MovieRepository;
import com.ptithcm.movie.movie.repository.SeasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeriesService {

    private final MovieRepository movieRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;
    private final CloudflareStreamService cloudflareStreamService;

    @Transactional
    public ServiceResult addSeason(UUID movieId, SeasonCreateRequest request) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phim"));

        if (!movie.isSeries()) {
            return ServiceResult.Failure().message("Chỉ có thể thêm mùa phim cho phim bộ");
        }

        if (seasonRepository.existsByMovieIdAndSeasonNumber(movieId, request.getSeasonNumber())) {
            return ServiceResult.Failure().message("Mùa phim với số thứ tự " + request.getSeasonNumber() + " đã tồn tại");
        }

        Season season = Season.builder()
                .movie(movie)
                .seasonNumber(request.getSeasonNumber())
                .title(request.getTitle())
                .build();

        Season savedSeason = seasonRepository.save(season);
        CreateSeasonDto created = new CreateSeasonDto();
        created.setId(season.getId());

        return ServiceResult.Success().code(ErrorCode.SUCCESS).data(created).message("Tạo mùa phim thành công");
    }

    @Transactional
    public ServiceResult addEpisode(UUID seasonId, EpisodeCreateRequest request) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mùa phim"));

        if (episodeRepository.existsBySeasonIdAndEpisodeNumber(seasonId, request.getEpisodeNumber())) {
            return ServiceResult.Failure().message("Tập phim với số thứ tự " + request.getEpisodeNumber() + " đã tồn tại trong mùa này");
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
        return ServiceResult.Success().code(ErrorCode.SUCCESS).message("Tạo tập phim thành công");
    }

    // --- UPDATE SEASON ---
    @Transactional
    public ServiceResult updateSeason(UUID id, SeasonUpdateRequest request) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mùa phim"));

        // Check trùng season number (nếu có thay đổi)
        if (request.getSeasonNumber() != null && !request.getSeasonNumber().equals(season.getSeasonNumber())) {
            boolean exists = seasonRepository.existsByMovieIdAndSeasonNumberAndIdNot(
                    season.getMovie().getId(),
                    request.getSeasonNumber(),
                    id
            );
            if (exists) {
                return ServiceResult.Failure().message("Số thứ tự mùa phim đã tồn tại");
            }
            season.setSeasonNumber(request.getSeasonNumber());
        }

        if (request.getTitle() != null) season.setTitle(request.getTitle());
        Season saved = seasonRepository.save(season);
        return ServiceResult.Success().code(ErrorCode.SUCCESS).message("Cập nhật mùa phim thành công");
    }

    // --- UPDATE EPISODE ---
    @Transactional
    public ServiceResult updateEpisode(UUID id, EpisodeUpdateRequest request) {
        Episode episode = episodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tập phim"));

        // Check trùng episode number
        if (request.getEpisodeNumber() != null && !request.getEpisodeNumber().equals(episode.getEpisodeNumber())) {
            boolean exists = episodeRepository.existsBySeasonIdAndEpisodeNumberAndIdNot(
                    episode.getSeason().getId(),
                    request.getEpisodeNumber(),
                    id
            );
            if (exists) {
                return ServiceResult.Failure().message("Số thứ tự tập phim đã tồn tại trong mùa này");
            }
            episode.setEpisodeNumber(request.getEpisodeNumber());
        }

        if (request.getTitle() != null) episode.setTitle(request.getTitle());
        if (request.getDurationMin() != null) episode.setDurationMin(request.getDurationMin());
        if (request.getSynopsis() != null) episode.setSynopsis(request.getSynopsis());
        if (request.getAirDate() != null) episode.setAirDate(request.getAirDate());
        if (request.getVideoUrl() != null) episode.setVideoUrl(request.getVideoUrl());
        Episode saved = episodeRepository.save(episode);
        return ServiceResult.Success().code(ErrorCode.SUCCESS).message("Câp nhật tập phim thành công");
    }

    @Transactional(readOnly = true)
    public ServiceResult getEpisodesBySeasonId(UUID seasonId) {
        if (!seasonRepository.existsById(seasonId)) {
            return ServiceResult.Failure()
                    .code(ErrorCode.FAILED)
                    .message("Không tìm thấy mùa phim yêu cầu");
        }

        boolean isLoggedIn = isAuthenticated();

        List<Episode> episodes = episodeRepository.findAllBySeasonIdOrderByEpisodeNumber(seasonId);

        List<MovieInfoResponse.EpisodeDto> dtos = episodes.stream().map(ep -> {
            String epSignedUrl = null;
            if (isLoggedIn && ep.getVideoUrl() != null) {
                String uid = ep.getVideoUrl();
                if (uid != null) {
                    epSignedUrl = cloudflareStreamService.generateSignedUrl(uid);
                }
            }

            return MovieInfoResponse.EpisodeDto.builder()
                    .id(ep.getId())
                    .episodeNumber(ep.getEpisodeNumber())
                    .title(ep.getTitle())
                    .duration(ep.getDurationMin())
                    .synopsis(ep.getSynopsis())
                    .stillPath(ep.getStillPath())
                    .airDate(ep.getAirDate())
                    .videoUrl(epSignedUrl)
                    .videoStatus(ep.getVideoStatus() != null ? String.valueOf(ep.getVideoStatus()) : "PENDING")
                    .build();
        }).toList();

        return ServiceResult.Success()
                .code(ErrorCode.SUCCESS)
                .data(dtos);
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }
}