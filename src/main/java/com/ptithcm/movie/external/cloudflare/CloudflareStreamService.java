package com.ptithcm.movie.external.cloudflare;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.StringReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class CloudflareStreamService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${cloudflare.key-id}")
    private String keyId;

    @Value("${cloudflare.private-key}")
    private String privateKeyPem;

    @Value("${cloudflare.subdomain}")
    private String subdomain;

    @Value("${cloudflare.account-id}")
    private String accountId;

    @Value("${cloudflare.api-token}")
    private String apiToken;


    /**
     * Tạo Signed URL theo chuẩn RS256 của Cloudflare
     */
    public String generateSignedUrl(String videoUid) {
        // 1. URL API của Cloudflare
        // Format: https://api.cloudflare.com/client/v4/accounts/{account_id}/stream/{video_uid}/token
        String apiUrl = String.format(
                "https://api.cloudflare.com/client/v4/accounts/%s/stream/%s/token",
                accountId, videoUid
        );

        try {
            // 2. Body Request (Giống hệt lệnh CURL)
            Map<String, Object> body = new HashMap<>();
            body.put("downloadable", false);
            body.put("accessRules", Collections.emptyList());
            body.put("nbf", 0); // Hiệu lực ngay lập tức

            // 3. Headers (Authorization: Bearer ...)
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 4. Gửi Request
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            // Gọi POST
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            // 5. Lấy Token từ kết quả trả về
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> result = (Map<String, Object>) response.getBody().get("result");
                String token = (String) result.get("token");

                // 6. Ghép thành Link hoàn chỉnh
                return String.format("https://%s/%s/manifest/video.m3u8",
                        subdomain,  token);
            }

        } catch (Exception e) {
            log.error("Lỗi khi gọi API Cloudflare lấy token cho video: {}", videoUid, e);
        }

        return null; // Trả về null nếu lỗi
    }
}