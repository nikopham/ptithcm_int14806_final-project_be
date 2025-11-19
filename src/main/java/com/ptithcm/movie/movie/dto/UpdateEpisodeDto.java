package com.ptithcm.movie.movie.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class UpdateEpisodeDto {
    private UUID id; // <-- ID (UUID) của Episode trong DB
    private String title;
    private Integer durationMin;
    private String synopsis;
    private String stillPath; // (URL mới nếu có, hoặc giữ nguyên)
    private LocalDate airDate;
    // (Bạn có thể thêm video_url nếu muốn)
}