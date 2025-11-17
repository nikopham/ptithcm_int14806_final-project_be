package com.ptithcm.movie.external.tmdb;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "tmdb")
@Getter
@Setter
public class TmdbConfig {
    private String apiKey;
    private String baseUrl;
}