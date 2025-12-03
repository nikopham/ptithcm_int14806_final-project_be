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
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudflareService {

    @Value("${cloudflare.account-id}")
    private String accountId;

    @Value("${cloudflare.api-token}")
    private String apiToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    public Map<String, String> getDirectUploadUrl() {
        String url = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/stream/direct_upload";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> data = new HashMap<>();
        data.put("maxDurationSeconds", 14_400);

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
        String url = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/stream/" + videoUid;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.get("success").asBoolean()) {
                JsonNode result = root.get("result");
                JsonNode status = result.get("status");

                Map<String, Object> info = new HashMap<>();
                info.put("uid", result.get("uid").asText());
                info.put("state", status.get("state").asText()); // ready, inprogress, queued

                // pctComplete có thể là string số hoặc null
                String pct = status.has("pctComplete") ? status.get("pctComplete").asText() : "0";
                info.put("pctComplete", Double.parseDouble(pct));

                info.put("readyToStream", result.get("readyToStream").asBoolean());

                return info;
            } else {
                throw new RuntimeException("Cloudflare Error: " + root.get("errors").toString());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get video details: " + e.getMessage());
        }
    }
}
