package com.ptithcm.movie.config;

import com.ptithcm.movie.external.meili.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MeiliSearchInitializer {

    private final SearchService searchService;

    @Bean
    public CommandLineRunner initMeiliSearchIndexes() {
        return args -> {
            System.out.println("🚀 Configuring Meilisearch indexes...");
            searchService.configureIndexes();
        };
    }
}