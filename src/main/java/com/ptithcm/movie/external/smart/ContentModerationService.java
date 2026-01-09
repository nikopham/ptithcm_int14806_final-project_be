package com.ptithcm.movie.external.smart;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class ContentModerationService {

    private final RestTemplate restTemplate = new RestTemplate();

//    private final String AI_URL = "http://localhost:5000/toxic/predict";
    private final String AI_URL = "http://ptithcm_int14806_final-project_ai.railway.internal:8080/toxic/predict";

    public ToxicCheckResponse analyzeContent(String content) {
        try {
            // Tạo body JSON
            Map<String, String> body = Map.of("text", content);

            ToxicCheckResponse response = restTemplate.postForObject(AI_URL, body, ToxicCheckResponse.class);

            if (response != null) {
                log.info("AI Check: [{}] -> Toxic: {} (Conf: {})", content, response.getIsToxic(), response.getConfidence());
                return response;
            }
        } catch (Exception e) {
            log.error("Lỗi kết nối AI Service: {}", e.getMessage());
        }

        // Fail-open: Nếu AI chết hoặc lỗi, trả về mặc định là SAFE và điểm 0
        return new ToxicCheckResponse(false, 0.0);
    }
}
