package com.ptithcm.movie.external.tmdb;

import com.ptithcm.movie.external.tmdb.dto.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class TMDbService {

    private final WebClient tmdbWebClient;
    private final String apiKey;

    @Autowired
    public TMDbService(
            @Qualifier("tmdbWebClient") WebClient tmdbWebClient,
            @Value("${tmdb.api-key}") String apiKey
    ) {
        this.tmdbWebClient = tmdbWebClient;
        this.apiKey = apiKey;
    }

    /**
     * API 1a (MỚI): Chỉ tìm kiếm MOVIE
     */
    @Cacheable(value = "tmdb-search-movie", key = "#query + '-' + #page")
    public MovieSearchResultDto searchMovies(String query, int page) {
        Mono<MovieSearchResultDto> responseMono = tmdbWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/movie") // Endpoint /search/movie
                        .queryParam("api_key", this.apiKey)
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .queryParam("language", "en-US")
                        .build())
                .retrieve()
                .bodyToMono(MovieSearchResultDto.class);

        return responseMono.block();
    }

    /**
     * API 1b (MỚI): Chỉ tìm kiếm TV SHOW
     */
    @Cacheable(value = "tmdb-search-tv", key = "#query + '-' + #page")
    public TvSearchResultDto searchTvShows(String query, int page) {
        Mono<TvSearchResultDto> responseMono = tmdbWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/tv") // Endpoint /search/tv
                        .queryParam("api_key", this.apiKey)
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .queryParam("language", "en-US")
                        .build())
                .retrieve()
                .bodyToMono(TvSearchResultDto.class);

        return responseMono.block();
    }

    /**
     * API 2a (Cập nhật): Lấy chi tiết MOVIE
     * (Thêm 'videos' và 'external_ids')
     */
    @Cacheable(value = "tmdb-movie-details", key = "#movieId")
    public MovieDetailDto getMovieFullDetails(int movieId) {
        Mono<MovieDetailDto> responseMono = tmdbWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/" + movieId)
                        .queryParam("api_key", this.apiKey)
                        .queryParam("language", "en-US")
                        // Gộp thêm: credits, videos, external_ids
                        .queryParam("append_to_response", "credits,videos,external_ids")
                        .build())
                .retrieve()
                .bodyToMono(MovieDetailDto.class);

        return responseMono.block();
    }

    /**
     * API 2b (Cập nhật): Lấy chi tiết TV SHOW
     * (Không cần adapter nữa, trả về DTO gốc)
     */
    @Cacheable(value = "tmdb-tv-details", key = "#tvId")
    public TvDetailDto getTvShowFullDetails(int tvId) {
        Mono<TvDetailDto> responseMono = tmdbWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tv/" + tvId)
                        .queryParam("api_key", this.apiKey)
                        .queryParam("language", "en-US")
                        // Gộp thêm: credits, videos, external_ids
                        .queryParam("append_to_response", "credits,videos,external_ids")
                        .build())
                .retrieve()
                .bodyToMono(TvDetailDto.class); // <-- Map vào DTO TV mới

        return responseMono.block();
    }
    @Cacheable(value = "tmdb-season-details", key = "#tvId + '-' + #seasonNumber")
    public TvSeasonDetailDto getTvSeasonDetails(int tvId, int seasonNumber) {
        Mono<TvSeasonDetailDto> responseMono = tmdbWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tv/" + tvId + "/season/" + seasonNumber) // Endpoint chi tiết season
                        .queryParam("api_key", this.apiKey)
                        .queryParam("language", "en-US")
                        .build())
                .retrieve() // Thực thi request
                .bodyToMono(TvSeasonDetailDto.class); // Map vào DTO Season

        return responseMono.block(); // Trả về kết quả
    }
}

