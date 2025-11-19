package com.ptithcm.movie.movie.repository;

import com.ptithcm.movie.movie.entity.Episode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EpisodeRepository extends JpaRepository<Episode, UUID> {
    // (Hiện tại không cần hàm tùy chỉnh)
}
