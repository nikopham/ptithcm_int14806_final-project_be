package com.ptithcm.movie.movie.service;

import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.movie.dto.UpdateEpisodeDto;
import com.ptithcm.movie.movie.dto.UpdateSeasonDto;
import com.ptithcm.movie.movie.entity.Episode;
import com.ptithcm.movie.movie.entity.Season;
import com.ptithcm.movie.movie.repository.EpisodeRepository;
import com.ptithcm.movie.movie.repository.SeasonRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SeasonService {

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private EpisodeRepository episodeRepository;

    @Transactional(rollbackFor = Exception.class)
    public ServiceResult updateSeason(UUID seasonId, UpdateSeasonDto dto) {
        try {
            // 1. Tìm Season
            Season season = seasonRepository.findById(seasonId)
                    .orElseThrow(() -> new EntityNotFoundException("Season not found"));

            // 2. Cập nhật Season
            season.setTitle(dto.getTitle());

            // 3. Cập nhật Episodes
            if (dto.getEpisodes() != null && !dto.getEpisodes().isEmpty()) {

                // Lấy tất cả ID episode từ DTO
                List<UUID> episodeIds = dto.getEpisodes().stream()
                        .map(UpdateEpisodeDto::getId)
                        .collect(Collectors.toList());

                // Lấy tất cả Episode Entity từ DB (để tối ưu)
                Map<UUID, Episode> episodeMap = episodeRepository.findAllById(episodeIds).stream()
                        .collect(Collectors.toMap(Episode::getId, Function.identity()));

                // Lặp qua DTO và cập nhật Entity
                for (UpdateEpisodeDto epDto : dto.getEpisodes()) {
                    Episode episode = episodeMap.get(epDto.getId());

                    // (An toàn) Chỉ cập nhật nếu episode thuộc đúng season
                    if (episode != null && episode.getSeason().getId().equals(seasonId)) {
                        episode.setTitle(epDto.getTitle());
                        episode.setDurationMin(epDto.getDurationMin());
                        episode.setSynopsis(epDto.getSynopsis());
                        episode.setStillPath(epDto.getStillPath());
                        episode.setAirDate(epDto.getAirDate());
                    }
                }

                // (Không cần saveAll() vì Entity đang ở trạng thái managed,
                //  nhưng save() ở dưới sẽ flush)
            }

            Season updatedSeason = seasonRepository.save(season);

            return ServiceResult.Success()
                    .message("Season updated successfully")
                    .data(updatedSeason);

        } catch (Exception e) {
            return ServiceResult.Failure()
                    .message("Failed to update season: " + e.getMessage());
        }
    }
}