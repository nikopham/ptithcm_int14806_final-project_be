package com.ptithcm.movie.external.meili;

import com.google.gson.Gson;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.model.SearchResult;
import com.meilisearch.sdk.model.SearchResultPaginated;
import com.meilisearch.sdk.model.Searchable;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.document.MovieDocument;
import com.ptithcm.movie.document.PersonDocument;
import com.ptithcm.movie.movie.entity.Genre;
import com.ptithcm.movie.movie.entity.Movie;
import com.ptithcm.movie.movie.entity.Person;
import com.ptithcm.movie.movie.repository.MovieRepository;
import com.ptithcm.movie.movie.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final Client client;
    private final Gson gson;
    private final MovieRepository movieRepository;
    private final PersonRepository personRepository;

    private static final String INDEX_MOVIES = "movies";
    private static final String INDEX_PEOPLE = "people";
    private static final int BATCH_SIZE = 1000;

    // --- 1. SETUP INDEX (Chạy 1 lần hoặc khi khởi động) ---
    public void configureIndexes() {
        try {
            // Config Movies
            Index movies = client.index(INDEX_MOVIES);
            movies.updateSearchableAttributesSettings(new String[]{"title", "originalTitle", "description"});
            movies.updateFilterableAttributesSettings(new String[]{"genres", "rating", "releaseYear", "isSeries", "status"});
            movies.updateSortableAttributesSettings(new String[]{"rating", "releaseYear"});

            // Config People
            Index people = client.index(INDEX_PEOPLE);
            people.updateSearchableAttributesSettings(new String[]{"fullName"});
            people.updateFilterableAttributesSettings(new String[]{"job"});

            log.info("✅ Meilisearch indexes configured!");
        } catch (Exception e) {
            log.error("❌ Failed to config Meilisearch: " + e.getMessage());
        }
    }

    // --- 2. SYNC MOVIES ---
    @Async
    public void indexMovie(Movie movie) {
        try {
            MovieDocument doc = MovieDocument.builder()
                    .id(movie.getId().toString())
                    .title(movie.getTitle())
                    .originalTitle(movie.getOriginalTitle())
                    .description(movie.getDescription())
                    .slug(movie.getSlug())
                    .poster(movie.getPosterUrl())
                    .rating(movie.getAverageRating())
                    .releaseYear(movie.getReleaseDate() != null ? movie.getReleaseDate().getYear() : 0)
                    .isSeries(movie.isSeries())
                    .status(String.valueOf(movie.getStatus()))
                    .genres(movie.getGenres().stream().map(Genre::getName).toList())
                    .build();

            client.index(INDEX_MOVIES).addDocuments(gson.toJson(List.of(doc)));
        } catch (Exception e) {
            log.error("Failed to index movie: " + movie.getTitle(), e);
        }
    }


    // --- 3. SYNC PEOPLE ---
    @Async
    public void indexPerson(Person person) {
        try {
            List<?> rawJobs = Optional.ofNullable(person.getJob()).orElse(Collections.emptyList());
            List<String> jobs = rawJobs.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toList());

            PersonDocument doc = PersonDocument.builder()
                    .id(person.getId().toString())
                    .fullName(person.getFullName())
                    .job(jobs)
                    .profilePath(person.getProfilePath())
                    .build();

            client.index(INDEX_PEOPLE).addDocuments(gson.toJson(List.of(doc)));
        } catch (Exception e) {
            log.error("Failed to index person", e);
        }
    }

    @Async // Chạy ngầm để api trả về nhanh
    public void removeMovie(UUID movieId) {
        try {
            // Xóa document có id tương ứng
            client.index(INDEX_MOVIES).deleteDocument(movieId.toString());
            log.info("Removed movie from Meilisearch: {}", movieId);
        } catch (Exception e) {
            log.error("Failed to remove movie from Meilisearch", e);
        }
    }

    // --- DELETE PERSON ---
    @Async
    public void removePerson(UUID personId) {
        try {
            client.index(INDEX_PEOPLE).deleteDocument(personId.toString());
            log.info("Removed person from Meilisearch: {}", personId);
        } catch (Exception e) {
            log.error("Failed to remove person from Meilisearch", e);
        }
    }


    // --- 4. GLOBAL SEARCH (Tìm cả 2 bảng cùng lúc) ---
    public ServiceResult searchMulti(String query) {
        try {
            // Chạy song song
            CompletableFuture<SearchResultPaginated> movieTask = CompletableFuture.supplyAsync(() -> {
                SearchRequest req = new SearchRequest(query)
                        .setPage(1)           // Dùng Page
                        .setHitsPerPage(5)    // Dùng HitsPerPage
                        .setAttributesToHighlight(new String[]{"title"})
                        .setFilter(new String[]{"status = PUBLISHED"});

                // Ép kiểu sang SearchResultPaginated
                return (SearchResultPaginated) client.index(INDEX_MOVIES).search(req);
            });

            CompletableFuture<SearchResultPaginated> personTask = CompletableFuture.supplyAsync(() -> {
                SearchRequest req = new SearchRequest(query)
                        .setPage(1)
                        .setHitsPerPage(5)
                        .setAttributesToHighlight(new String[]{"fullName"});

                // Ép kiểu sang SearchResultPaginated
                return (SearchResultPaginated) client.index(INDEX_PEOPLE).search(req);
            });

            CompletableFuture.allOf(movieTask, personTask).join();

            Map<String, Object> result = new HashMap<>();

            // SearchResultPaginated có hàm getHits() trả về ArrayList
            result.put("movies", movieTask.get().getHits());
            result.put("people", personTask.get().getHits());

            return ServiceResult.Success().data(result);

        } catch (Exception e) {
            // e.getMessage() có thể bị wrap bởi CompletableFuture, nên log kỹ
            log.error("Search error", e);
            return ServiceResult.Failure().message("Search failed: " + e.getMessage());
        }
    }

    // ========================================================================
    // LOGIC ĐỒNG BỘ TOÀN BỘ (FULL SYNC)
    // ========================================================================

    @Async
    public void syncAllData() {
        long startTime = System.currentTimeMillis();
        log.info("🔄 STARTING FULL SYNC...");

        try {
//             1. Xóa sạch dữ liệu cũ trong Index (Optional - Cẩn thận khi dùng)
             client.index(INDEX_MOVIES).deleteAllDocuments();
             client.index(INDEX_PEOPLE).deleteAllDocuments();

            // 2. Đồng bộ Movies
            syncAllMovies();

            // 3. Đồng bộ People
            syncAllPeople();

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ FULL SYNC COMPLETED in {} ms", duration);

        } catch (Exception e) {
            log.error("❌ Full sync failed", e);
        }
    }

    private void syncAllMovies() {
        int page = 0;
        Page<Movie> moviePage;

        log.info("--- Syncing Movies ---");
        do {
            // Lấy từng trang (Batch) từ DB
            moviePage = movieRepository.findAll(PageRequest.of(page, BATCH_SIZE));

            if (moviePage.hasContent()) {
                // Convert Entity -> Document
                List<MovieDocument> docs = moviePage.getContent().stream()
                        .map(this::mapToMovieDocument) // Hàm helper bên dưới
                        .toList();

                // Đẩy lên Meilisearch
                try {
                    client.index(INDEX_MOVIES).addDocuments(gson.toJson(docs));
                    log.info("Movies batch {}/{} indexed ({} items)",
                            page + 1, moviePage.getTotalPages(), docs.size());
                } catch (Exception e) {
                    log.error("Failed to index movie batch " + page, e);
                }
            }
            page++;
        } while (moviePage.hasNext()); // Lặp cho đến khi hết trang
    }

    private void syncAllPeople() {
        int page = 0;
        Page<Person> personPage;

        log.info("--- Syncing People ---");
        do {
            personPage = personRepository.findAll(PageRequest.of(page, BATCH_SIZE));

            if (personPage.hasContent()) {
                List<PersonDocument> docs = personPage.getContent().stream()
                        .map(this::mapToPersonDocument)
                        .toList();

                try {
                    client.index(INDEX_PEOPLE).addDocuments(gson.toJson(docs));
                    log.info("People batch {}/{} indexed ({} items)",
                            page + 1, personPage.getTotalPages(), docs.size());
                } catch (Exception e) {
                    log.error("Failed to index person batch " + page, e);
                }
            }
            page++;
        } while (personPage.hasNext());
    }

    // ========================================================================
    // HELPER MAPPERS (Tách ra để dùng chung cho cả sync lẻ và sync all)
    // ========================================================================

    private MovieDocument mapToMovieDocument(Movie movie) {
        // Lưu ý: Cẩn thận Lazy Loading ở đây (genres)
        // Nếu movieRepository.findAll không fetch join, dòng này sẽ gây N+1 query nhẹ
        // Nhưng với batch processing thì chấp nhận được.
        List<String> genreNames = movie.getGenres().stream()
                .map(Genre::getName)
                .toList();

        return MovieDocument.builder()
                .id(movie.getId().toString())
                .title(movie.getTitle())
                .originalTitle(movie.getOriginalTitle())
                .description(movie.getDescription())
                .slug(movie.getSlug())
                .poster(movie.getPosterUrl())
                .rating(movie.getAverageRating())
                .releaseYear(movie.getReleaseDate() != null ? movie.getReleaseDate().getYear() : 0)
                .isSeries(movie.isSeries())
                .status(String.valueOf(movie.getStatus()))
                .genres(genreNames)
                .build();
    }

    private PersonDocument mapToPersonDocument(Person person) {
        return PersonDocument.builder()
                .id(person.getId().toString())
                .fullName(person.getFullName())
                .job(person.getJob())
                .profilePath(person.getProfilePath())
                .build();
    }

}