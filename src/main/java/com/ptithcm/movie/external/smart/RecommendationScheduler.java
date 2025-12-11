package com.ptithcm.movie.external.smart;

import com.ptithcm.movie.movie.repository.ViewingHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendationScheduler {
    private final ViewingHistoryRepository historyRepo;
    private final RestTemplate restTemplate = new RestTemplate();

    // Chạy lúc 3:00 sáng mỗi ngày
    @Scheduled(cron = "0 0 3 * * *")
    public void runTraining() {
        log.info("🔄 Bắt đầu Job Training...");

        // 1. Lấy dữ liệu từ DB
        List<Map<String, Object>> raw = historyRepo.getTrainingData();

        // 2. Map sang DTO
        List<UserRatingDto> data = raw.stream().map(row -> new UserRatingDto(
                (String) row.get("userId"), (String) row.get("movieId"), (Double) row.get("finalRating")
        )).toList();

        // 3. Gửi sang Python
        try {
            restTemplate.postForEntity("http://localhost:5000/movie/train", Map.of("data", data), Void.class);
            log.info("✅ Training thành công!");
        } catch (Exception e) {
            log.error("❌ Lỗi Training: ", e);
        }
    }
}