package com.ptithcm.movie.external.smart;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.model.SearchResult;
import com.ptithcm.movie.external.meili.SearchService;
import com.ptithcm.movie.movie.dto.response.MovieInfoResponse;
import com.ptithcm.movie.movie.dto.response.MovieShortResponse;
import com.ptithcm.movie.movie.entity.Genre;
import com.ptithcm.movie.movie.entity.Movie;
import com.ptithcm.movie.movie.entity.MovieLike;
import com.ptithcm.movie.movie.entity.ViewingHistory;
import com.ptithcm.movie.movie.repository.MovieLikeRepository;
import com.ptithcm.movie.movie.repository.MovieRepository;
import com.ptithcm.movie.movie.repository.ViewingHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final MovieRepository movieRepo;
    private final MovieLikeRepository likeRepo;
    private final ViewingHistoryRepository historyRepo;
    private final RestTemplate restTemplate = new RestTemplate();
    private final SearchService searchService;
    private final Client meiliClient;

    public List<MovieShortResponse> getRecommendations(UUID userId) {
        Set<UUID> finalIds = new LinkedHashSet<>();

        try {
            List<String> allIds = movieRepo.findAllIdsNotDraft().stream().map(UUID::toString).toList();
            Map<String, Object> body = Map.of("userId", userId.toString(), "allMovieIds", allIds);

//            ResponseEntity<Map> res = restTemplate.postForEntity("http://localhost:5000/movie/recommend", body, Map.class);
            ResponseEntity<Map> res = restTemplate.postForEntity("http://ptithcm_int14806_final-project_ai.railway.internal:8080/movie/recommend", body, Map.class);

            List<String> svdIds = (List<String>) res.getBody().get("movieIds");

            svdIds.stream().limit(7).forEach(id -> finalIds.add(UUID.fromString(id)));
        } catch (Exception e) {
            log.warn("Python SVD failed: {}", e.getMessage());
        }

        // --- BƯỚC 2: CONTENT-BASED (DÙNG MEILISEARCH) ---
        Optional<MovieLike> lastLike = likeRepo.findTopByUserIdOrderByCreatedAtDesc(userId);
        Movie seedMovie = null;

        if (lastLike.isPresent()) {
            seedMovie = movieRepo.findById(lastLike.get().getMovie().getId()).orElse(null);
        } else {
            Optional<ViewingHistory> lastWatch = historyRepo.findTopByUserIdOrderByLastWatchedAtDesc(userId);
            if (lastWatch.isPresent()) seedMovie = lastWatch.get().getMovie();
        }

        if (seedMovie != null) {
            try {
                List<String> genreNames = seedMovie.getGenres().stream().map(Genre::getName).toList();

                // Tạo câu filter: (genres = 'Action' OR genres = 'Comedy') AND id != 'uuid-hien-tai'
                String filterQuery = buildMeiliFilter(genreNames, seedMovie.getId().toString());

                // Tìm kiếm rỗng ("") để lấy tất cả phim thỏa mãn filter
                SearchResult result = (SearchResult) meiliClient.index("movies")
                        .search(new SearchRequest("")
                                .setFilter(new String[]{ filterQuery })
                                .setLimit(10) // Lấy dư ra một chút để fill
                        );

                // Add kết quả vào list
                result.getHits().stream()
                        .map(hit -> (String) hit.get("id")) // Lấy ID từ JSON
                        .map(UUID::fromString)
                        .forEach(id -> {
                            if (finalIds.size() < 10) {
                                finalIds.add(id);
                            }
                        });

            } catch (Exception e) {
                log.error("MeiliSearch failed: {}", e.getMessage());
                // Fallback: Nếu Meili chết, có thể gọi lại SQL cũ ở đây hoặc bỏ qua
            }
        }

        // --- BƯỚC 3: TRENDING (Giữ nguyên) ---
        if (finalIds.isEmpty()) {
            return movieRepo.findTop10ByOrderByViewCountDesc().stream().map(this::mapToDto).toList();
        }


        if (finalIds.isEmpty()) {
            return movieRepo.findTop10ByOrderByViewCountDesc().stream().map(this::mapToDto).toList();
        }

        List<Movie> movies = movieRepo.findAllById(finalIds);

        Map<UUID, Movie> movieMap = movies.stream()
                .collect(Collectors.toMap(Movie::getId, Function.identity()));

        return finalIds.stream()
                .map(movieMap::get)
                .filter(Objects::nonNull)
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Helper: Tạo chuỗi Filter cho MeiliSearch
     * Output ví dụ: "(genres = 'Action' OR genres = 'Drama') AND id != '123-abc'"
     */
    private String buildMeiliFilter(List<String> genreNames, String excludeId) {
        String baseCondition = "id != '" + excludeId + "' AND status != 'DRAFT'";

        if (genreNames.isEmpty()) {
            return baseCondition;
        }

        String genreCondition = genreNames.stream()
                .map(g -> "genres = '" + g + "'") // Cú pháp filter của MeiliSearch
                .collect(Collectors.joining(" OR "));

        return "(" + genreCondition + ") AND " + baseCondition;
    }


    private MovieShortResponse mapToDto(Movie movie) {
        if (movie == null) return null;

        return MovieShortResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .posterUrl(movie.getPosterUrl())
                .releaseYear(movie.getReleaseDate() != null ? movie.getReleaseDate().getYear() : null)
                .slug(movie.getSlug())
                .build();
    }
}
