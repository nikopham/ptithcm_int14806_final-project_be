package com.ptithcm.movie.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtConfig {
    private String secret;          // HMAC-SHA256
    private long  accessTtlMin;     // 15
    private long  refreshTtlDay;    // 7
    private String refreshCookie;   // "refresh_token"
    private boolean cookieSecure;
}
