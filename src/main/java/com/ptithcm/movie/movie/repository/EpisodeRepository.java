package com.ptithcm.movie.movie.repository;

import com.ptithcm.movie.movie.entity.Episode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EpisodeRepository extends JpaRepository<Episode, UUID> {
    boolean existsBySeasonIdAndEpisodeNumber(UUID seasonId, Integer episodeNumber);
    boolean existsBySeasonIdAndEpisodeNumberAndIdNot(UUID seasonId, Integer episodeNumber, UUID id);
    Optional<Episode> findByVideoUrlContaining(String videoUid);
}