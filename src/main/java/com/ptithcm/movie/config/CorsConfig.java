package com.ptithcm.movie.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Tách CORS ra cho gọn – có thể bật/tắt hoặc thay đổi origin qua application.yaml.
 */
@Configuration
@ConfigurationProperties(prefix = "app.cors")
@Getter @Setter
public class CorsConfig {

    /**
     * List tên miền frontend được phép (VD: http://localhost:5173, https://movies.ptithcm.com).
     * Đọc từ application.yaml cấu hình runtime.
     */
    private List<String> allowedOrigins = List.of("http://localhost:5173", "https://ptithcm-int-14806-final-project-43upmrm53.vercel.app");

    private List<String> allowedMethods = List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS");
    private List<String> allowedHeaders = List.of(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
    );

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(allowedOrigins);
        cfg.setAllowedMethods(allowedMethods);
        cfg.setAllowedHeaders(allowedHeaders);
        cfg.setAllowCredentials(true);          // cần cho Cookie refresh-token
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}

