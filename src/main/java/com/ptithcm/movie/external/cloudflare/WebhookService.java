package com.ptithcm.movie.external.cloudflare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptithcm.movie.common.constant.MovieStatus;
import com.ptithcm.movie.common.constant.VideoUploadStatus;
import com.ptithcm.movie.movie.entity.Episode;
import com.ptithcm.movie.movie.entity.Movie;
import com.ptithcm.movie.movie.repository.EpisodeRepository;
import com.ptithcm.movie.movie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final MovieRepository movieRepository;
    private final EpisodeRepository episodeRepository;
    private final ObjectMapper objectMapper;

    @Value("${cloudflare.webhook-secret}")
    private String webhookSecret;

    @Value("${cloudflare.subdomain}")
    private String cloudflareSubdomain;

    /**
     * Xử lý Webhook từ Cloudflare
     */
    public void handleCloudflareWebhook(String signatureHeader, String body) {
        // 1. Kiểm tra Signature (Bảo mật)
        if (!verifySignature(signatureHeader, body)) {
            log.error("❌ Invalid Webhook Signature! Request rejected.");
            throw new RuntimeException("Invalid Webhook Signature");
        }

        try {
            // 2. Parse JSON Body
            JsonNode root = objectMapper.readTree(body);

            // Cloudflare trả về nhiều loại event, ta lấy type của event đó
            // VD: "video.ready", "video.created"
            // Lưu ý: Cấu trúc JSON thực tế của Cloudflare có thể bọc trong mảng hoặc object tùy config
            // Ở đây ta giả định format chuẩn: { "uid": "...", "status": { "state": "ready" } }

            String uid = root.get("uid").asText();
            JsonNode statusNode = root.get("status");
            String state = statusNode.get("state").asText();

            int height = 0;
            double duration = 0.0;

            JsonNode inputNode = root.path("input");
            if (!inputNode.isMissingNode()) {
                height = inputNode.path("height").asInt(0);
                // Cloudflare trả về duration tính bằng giây
                duration = root.path("duration").asDouble(0.0);
            }

            log.info("🔔 Webhook received: UID={}, State={}, Height={}", uid, state, height);

            if ("ready".equalsIgnoreCase(state)) {
                // [MỚI] Truyền thêm height và duration vào hàm update
                updateVideoStatus(uid, height, duration);
            } else if ("error".equalsIgnoreCase(state)) {
                log.error("Video processing failed for UID: {}", uid);
                handleVideoError(uid);
            }

        } catch (Exception e) {
            log.error("Error processing webhook body", e);
            throw new RuntimeException("Webhook processing error");
        }
    }

    private void handleVideoError(String uid) {
        log.warn("⚠️ Handling video ERROR for UID: {}", uid);

        // 1. Tìm trong bảng Movie trước
        Optional<Movie> movieOpt = movieRepository.findByVideoUrlContaining(uid);
        if (movieOpt.isPresent()) {
            Movie movie = movieOpt.get();

            // Chỉ update nếu chưa phải là FAILED
            if (movie.getVideoStatus() != VideoUploadStatus.FAILED) {
                movie.setVideoStatus(VideoUploadStatus.FAILED);
                movieRepository.save(movie);
                log.error("❌ Movie '{}' processing FAILED. UID: {}", movie.getTitle(), uid);
            }
            return; // Tìm thấy rồi thì dừng
        }

        // 2. Nếu không phải Movie, tìm trong bảng Episode
        Optional<Episode> episodeOpt = episodeRepository.findByVideoUrlContaining(uid);
        if (episodeOpt.isPresent()) {
            Episode episode = episodeOpt.get();

            // Cập nhật trạng thái FAILED (Yêu cầu Entity Episode phải có field videoStatus)
            // Nếu Entity Episode chưa có field này, bạn cần thêm vào hoặc chấp nhận chỉ log lỗi
            // episode.setVideoStatus(VideoUploadStatus.FAILED);

            // Nếu chưa có field status, ta có thể đánh dấu tạm vào videoUrl hoặc log ra
            log.error("❌ Episode '{}' (Season {}) processing FAILED. UID: {}",
                    episode.getTitle(), episode.getSeason().getSeasonNumber(), uid);

            // Ví dụ: Nếu Episode có cột status
            // episode.setVideoStatus(VideoUploadStatus.FAILED);
            // episodeRepository.save(episode);
            return;
        }

        // 3. Không tìm thấy ở đâu cả
        log.warn("⚠️ Received ERROR webhook for unknown UID: {}", uid);
    }

    /**
     * Hàm xác định chuẩn chất lượng dựa trên chiều cao (px)
     */
    private String determineQuality(int height) {
        if (height >= 2160) return "4K";
        if (height >= 1440) return "2K";
        if (height >= 1080) return "1080P";
        if (height >= 720) return "720P";
        if (height >= 480) return "480P";
        return "240P"; // Hoặc Unknown
    }

    private void updateVideoStatus(String uid, int height, double durationSeconds) {
        String hlsUrl = String.format(
                "https://customer-%s.cloudflarestream.com/%s/manifest/video.m3u8",
                cloudflareSubdomain, uid
        );

        // Tính toán chất lượng (4K, FHD...)
        String quality = determineQuality(height);

        // --- TRƯỜNG HỢP 1: LÀ MOVIE ---
        Optional<Movie> movieOpt = movieRepository.findByVideoUrlContaining(uid);
        if (movieOpt.isPresent()) {
            Movie movie = movieOpt.get();

            // Chỉ update nếu chưa Ready để tránh spam DB
            if (movie.getVideoStatus() != VideoUploadStatus.READY) {
                movie.setVideoUrl(hlsUrl);
                movie.setVideoStatus(VideoUploadStatus.READY);
                movie.setQuality(quality); // [MỚI] Lưu chất lượng

                // [MỚI] Cập nhật thời lượng chính xác từ file video (nếu muốn)
                // movie.setDurationMin((int) (durationSeconds / 60));

                movieRepository.save(movie);
                log.info("✅ MOVIE '{}' is READY ({}).", movie.getTitle(), quality);
            }
            return; // Tìm thấy Movie rồi thì dừng, không tìm Episode nữa
        }

        // --- TRƯỜNG HỢP 2: LÀ EPISODE (TV SERIES) ---
        // Bạn cần thêm hàm findByVideoUrlContaining trong EpisodeRepository
        Optional<Episode> episodeOpt = episodeRepository.findByVideoUrlContaining(uid);
        if (episodeOpt.isPresent()) {
            Episode episode = episodeOpt.get();

            // Giả sử Episode entity cũng có cột videoStatus (nếu chưa có thì chỉ update URL)
            // episode.setVideoStatus(VideoUploadStatus.READY);
            episode.setVideoUrl(hlsUrl);

            // episode.setDurationMin((int) (durationSeconds / 60));

            episodeRepository.save(episode);
            log.info("✅ EPISODE '{}' is READY.", episode.getTitle());
            return;
        }

        log.warn("⚠️ No Movie or Episode found with UID: {}", uid);
    }

    /**
     * Logic xác thực chữ ký chuẩn theo Document của Cloudflare
     * Header format: time=1234567890,sig1=abcdef...
     */
    private boolean verifySignature(String signatureHeader, String body) {
        if (signatureHeader == null) return false;

        try {
            // Tách time và signature từ header
            String[] parts = signatureHeader.split(",");
            String time = null;
            String sig1 = null;

            for (String part : parts) {
                if (part.startsWith("time=")) time = part.substring(5);
                if (part.startsWith("sig1=")) sig1 = part.substring(5);
            }

            if (time == null || sig1 == null) return false;

            // Tạo chuỗi cần hash: {time}.{body}
            String stringToSign = time + "." + body;

            // Hash bằng HMAC-SHA256 với Secret Key
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));

            // Chuyển bytes sang Hex String để so sánh
            StringBuilder generatedSig = new StringBuilder();
            for (byte b : hmacBytes) {
                generatedSig.append(String.format("%02x", b));
            }

            // So sánh chữ ký tính được với chữ ký Cloudflare gửi
            return generatedSig.toString().equals(sig1);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Signature verification error", e);
            return false;
        }
    }
}