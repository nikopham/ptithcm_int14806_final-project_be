package com.ptithcm.movie.movie.tmdb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptithcm.movie.movie.tmdb.dto.TmdbMovieBrief;
import com.ptithcm.movie.movie.tmdb.dto.TmdbMovieDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Service
public class TmdbClient {

    private final ObjectMapper objectMapper;
    private final WebClient    client;
    private final String       apiKey;

    public TmdbClient(TmdbConfig cfg,
                      WebClient.Builder builder,
                      ObjectMapper mapper) {

        this.objectMapper = mapper;
        this.apiKey = cfg.getApiKey();
        this.client = builder
                .baseUrl(cfg.getBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Flux<TmdbMovieBrief> searchMovies(String query, int page) {
        return client.get()
                .uri(uriBuilder -> uriBuilder.path("/search/movie")
                        .queryParam("api_key", apiKey)
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMapMany(json -> Flux.fromIterable(json.get("results")))
                .map(node -> objectMapper.convertValue(node, TmdbMovieBrief.class));
    }

    public Mono<TmdbMovieDetail> getMovie(Integer tmdbId) {
        return client.get()
                .uri("/movie/{id}?api_key={key}", tmdbId, apiKey)
                .retrieve()
                .bodyToMono(TmdbMovieDetail.class);
    }
}