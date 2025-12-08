package com.ptithcm.movie.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;

@Configuration
public class MeiliConfig {

    @Value("${meilisearch.host}")
    private String host;

    @Value("${meilisearch.api-key}")
    private String apiKey;

    @Bean
    public Client meilisearchClient() {
        return new Client(new Config(host, apiKey));
    }
}