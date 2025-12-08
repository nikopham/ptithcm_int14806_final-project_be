package com.ptithcm.movie.movie.dto.request;

import lombok.Data;
import java.util.UUID;

@Data
public class WatchProgressRequest {
    private UUID movieId;
    private UUID episodeId; // Null nếu là Movie lẻ
    private Integer watchedSeconds; // Thời điểm hiện tại (ví dụ giây thứ 300)
    private Integer totalSeconds;   // Tổng thời lượng (ví dụ 5400 giây)
}