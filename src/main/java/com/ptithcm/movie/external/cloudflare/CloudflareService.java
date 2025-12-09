package com.ptithcm.movie.external.cloudflare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudflareService {

    @Value("${cloudflare.account-id}")
    private String accountId;

    @Value("${app.public-url}")
    private String publicUrl;

    @Value("${cloudflare.api-token}")
    private String apiToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    public Map<String, String> getDirectUploadUrl() {
        String url = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/stream/direct_upload";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Tus-Resumable", "1.0.0");
        headers.set("Upload-Length", "0");

        Map<String, Object> data = new HashMap<>();
        data.put("maxDurationSeconds", 14_400);
        data.put("requireSignedURLs", true);
        data.put("allowedOrigins", List.of("your-movie-website.com", "localhost:5173"));
        String webhookUrl = publicUrl + "/api/v1/webhooks/cloudflare";
        data.put("notificationUrl", webhookUrl);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(data, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            log.info("Cloudflare response status: {}, body: {}",
                    response.getStatusCode(), response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Non-2xx from Cloudflare: " + response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.path("success").asBoolean(false)) {
                JsonNode result = root.path("result");
                return Map.of(
                        "uploadUrl", result.path("uploadURL").asText(),
                        "videoUID", result.path("uid").asText()
                );
            } else {
                throw new RuntimeException("Cloudflare Error: " + root.path("errors").toString());
            }

        } catch (HttpStatusCodeException e) {
            // Lỗi HTTP (401, 403, 404, 500, ...)
            log.error("Cloudflare HTTP error. Status: {}, Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to get upload URL from Cloudflare (HTTP)", e);
        } catch (Exception e) {
            log.error("Cloudflare unexpected error", e);
            throw new RuntimeException("Failed to get upload URL from Cloudflare", e);
        }
    }

    public Map<String, Object> getVideoDetails(String videoUid) {
        // 1. Validate UID trước khi gọi
        if (videoUid == null || videoUid.trim().isEmpty()) {
            throw new RuntimeException("Video UID cannot be empty");
        }

        String url = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/stream/" + videoUid;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            // 2. Check HTTP Status
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Cloudflare Error Status: {}", response.getStatusCode());
                throw new RuntimeException("Cloudflare returned non-200 status: " + response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.path("success").asBoolean(false)) {
                JsonNode result = root.path("result");
                JsonNode status = result.path("status");

                // Lấy thông tin an toàn bằng .path() để tránh NullPointerException
                int height = result.path("input").path("height").asInt(0);
                int width = result.path("input").path("width").asInt(0);
                double duration = result.path("duration").asDouble(0.0);

                Map<String, Object> info = new HashMap<>();
                info.put("uid", result.path("uid").asText());
                info.put("state", status.path("state").asText());
                info.put("height", height);
                info.put("width", width);
                info.put("duration", duration);

                String pct = status.path("pctComplete").asText("0");
                info.put("pctComplete", Double.parseDouble(pct));
                info.put("readyToStream", result.path("readyToStream").asBoolean());

                return info;
            } else {
                throw new RuntimeException("Cloudflare Error: " + root.path("errors").toString());
            }
        } catch (HttpStatusCodeException e) {
            // Bắt lỗi 4xx, 5xx cụ thể từ RestTemplate
            log.error("Cloudflare HTTP Error: {} - Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to call Cloudflare: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Internal Error calling Cloudflare", e);
            throw new RuntimeException("Failed to get video details: " + e.getMessage());
        }
    }
}
