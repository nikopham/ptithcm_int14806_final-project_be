package com.ptithcm.movie.movie.dto.request;

import lombok.Data;
import java.util.UUID;

@Data
public class WatchProgressRequest {
    private UUID movieId;
    private UUID episodeId; // Null nếu là Movie lẻ
    private Long currentSecond;
    private Long totalSeconds;
    private Long watchedDelta;
}