package com.ptithcm.movie.auth.session;


import com.ptithcm.movie.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Lưu / kiểm tra JTI của refresh-token trong Redis để có thể revoke.
 * Key pattern:  session:{userId}:{jti} -> 1
 */
@Service
@RequiredArgsConstructor
public class UserSessionService {

    private final StringRedisTemplate redis;
    private final JwtConfig cfg;

    /* tạo session – TTL = refreshTtlDay */
    public UUID createSession(UUID userId, String userAgent) {
        UUID jti = UUID.randomUUID();
        String key = redisKey(userId, jti);
        ValueOperations<String, String> ops = redis.opsForValue();
        ops.set(key, userAgent, Duration.ofDays(cfg.getRefreshTtlDay()));
        return jti;
    }

    /* kiểm tra còn hợp lệ */
    public boolean isActive(UUID userId, UUID jti) {
        return Boolean.TRUE.equals(redis.hasKey(redisKey(userId, jti)));
    }

    /* revoke (logout) */
    public void revoke(UUID userId, UUID jti) {
        redis.delete(redisKey(userId, jti));
    }

    private String redisKey(UUID user, UUID jti) {
        return "session:" + user + ":" + jti;
    }
}