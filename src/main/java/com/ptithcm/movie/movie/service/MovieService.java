package com.ptithcm.movie.movie.service;

import com.ptithcm.movie.auth.security.UserPrincipal;
import com.ptithcm.movie.comment.repository.MovieCommentRepository;
import com.ptithcm.movie.common.constant.AgeRating;
import com.ptithcm.movie.common.constant.ErrorCode;
import com.ptithcm.movie.common.constant.MovieStatus;
import com.ptithcm.movie.common.constant.VideoUploadStatus;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.external.cloudflare.CloudflareService;
import com.ptithcm.movie.external.cloudflare.CloudflareStreamService;
import com.ptithcm.movie.external.cloudinary.CloudinaryService;
import com.ptithcm.movie.external.meili.SearchService;
import com.ptithcm.movie.movie.dto.request.MovieCreateRequest;
import com.ptithcm.movie.movie.dto.request.MovieSearchRequest;
import com.ptithcm.movie.movie.dto.request.MovieUpdateRequest;
import com.ptithcm.movie.movie.dto.request.WatchProgressRequest;
import com.ptithcm.movie.movie.dto.response.*;
import com.ptithcm.movie.movie.entity.*;
import com.ptithcm.movie.movie.repository.*;
import com.ptithcm.movie.user.entity.User;
import com.ptithcm.movie.user.repository.UserRepository;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieService {

    private final MovieRepository movieRepository;
    private final CloudinaryService cloudinaryService;
    private final GenreRepository genreRepository;
    private final CountryRepository countryRepository;
    private final PersonRepository personRepository;
    private final SeasonRepository seasonRepository;
    private final ReviewRepository reviewRepository;
    private final ViewingHistoryRepository historyRepository;
    private final MovieCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final MovieLikeRepository movieLikeRepository;
    private final CloudflareService cloudflareService;
    private final SearchService searchService;
    private final EpisodeRepository episodeRepository;
    private final CloudflareStreamService cloudflareStreamService;

    @Transactional
    public void increaseViewCount(UUID movieId) {
        movieRepository.incrementViewCount(movieId);
    }

    @Transactional
    public void saveProgress(WatchProgressRequest request) {
        User user = getCurrentUser();
        if (user == null) return;

        ViewingHistory history = historyRepository
                .findByUserIdAndMovieIdAndEpisodeId(user.getId(), request.getMovieId(), request.getEpisodeId())
                .orElse(null);

        if (history == null) {
            Movie movie = movieRepository.getReferenceById(request.getMovieId());
            Episode episode = request.getEpisodeId() != null ? episodeRepository.getReferenceById(request.getEpisodeId()) : null;

            history = ViewingHistory.builder()
                    .user(user)
                    .movie(movie)
                    .episode(episode)
                    .accumulatedSeconds(0L)
                    .isCounted(false)
                    .build();
        }

        history.setCurrentSecond(request.getCurrentSecond());

        history.setTotalSeconds(request.getTotalSeconds());
        history.setLastWatchedAt(OffsetDateTime.now());

        long validDelta = 0L;
        if (request.getWatchedDelta() != null
                && request.getWatchedDelta() > 0
                && request.getWatchedDelta() <= 20) {
            validDelta = request.getWatchedDelta();
        }

        long currentAccumulated = history.getAccumulatedSeconds() == null ? 0L : history.getAccumulatedSeconds();
        long newAccumulated = currentAccumulated + validDelta;

        if (request.getTotalSeconds() != null && newAccumulated > request.getTotalSeconds()) {
            newAccumulated = request.getTotalSeconds();
        }

        history.setAccumulatedSeconds(newAccumulated);

        if (request.getTotalSeconds() != null && request.getTotalSeconds() > 0) {
            // Tính ngưỡng 10% (1/10)
            double threshold = request.getTotalSeconds() * 0.1;

            // Điều kiện:
            // 1. Thời gian tích lũy >= 10% tổng thời lượng
            // 2. Chưa từng cộng view cho lần xem này (!isCounted)
            if (newAccumulated >= threshold && !Boolean.TRUE.equals(history.getIsCounted())) {

                movieRepository.incrementViewCount(request.getMovieId());

                history.setIsCounted(true);
            }
        }

        historyRepository.save(history);
    }

    public ServiceResult getReleaseYears() {
        List<Integer> years = movieRepository.findDistinctReleaseYears();
        return ServiceResult.Success().data(years);
    }

    @Transactional
    public ServiceResult deleteMovie(UUID id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movie not found"));
        if (historyRepository.existsByMovieId(id)) {
            return ServiceResult.Failure()
                    .code(ErrorCode.FAILED)
                    .message("Không thể xóa phim. Đã có lịch sử xem.");
        }
        if (reviewRepository.existsByMovieId(id)) {
            return ServiceResult.Failure()
                    .code(ErrorCode.FAILED)
                    .message("Không thể xóa phim. Đã có đánh giá.");
        }
        if (commentRepository.existsByMovieId(id)) {
            return ServiceResult.Failure()
                    .code(ErrorCode.FAILED)
                    .message("Không thể xóa phim. Đã có bình luận.");
        }

        String posterUrl = movie.getPosterUrl();
        String backdropUrl = movie.getBackdropUrl();

        if(movie.getVideoUrl() != null){
            cloudflareService.deleteVideo(movie.getVideoUrl());
        }

        movieRepository.delete(movie);

        movieRepository.flush();

        searchService.removeMovie(id);

        CompletableFuture.runAsync(() -> {
            cloudinaryService.deleteImage(posterUrl);
            cloudinaryService.deleteImage(backdropUrl);
        });

        return ServiceResult.Success().code(ErrorCode.SUCCESS)
                .message("Xóa phim thành công");
    }

    @Transactional
    public ServiceResult updateMovie(UUID id, MovieUpdateRequest request) {
        try {
            Optional<Movie> existingMovie = movieRepository.findMovieByTitleIgnoreCase(request.getTitle());
            if (existingMovie.isPresent()) {
                return ServiceResult.Failure()
                        .code(ErrorCode.FAILED)
                        .message("Phim với tiêu đề '" + request.getTitle() + "' đã tồn tại.");
            }
            Movie movie = movieRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phim"));

            // 1. Update Basic Info (Chỉ update nếu khác null)
            if (request.getTitle() != null) movie.setTitle(request.getTitle());
            if (request.getOriginalTitle() != null) movie.setOriginalTitle(request.getOriginalTitle());
            if (request.getDescription() != null) movie.setDescription(request.getDescription());
            if (request.getDurationMin() != null) movie.setDurationMin(request.getDurationMin());
            if (request.getVideoUrl() != null) movie.setVideoUrl(request.getVideoUrl());
            // Enum
            if (request.getAgeRating() != null) movie.setAgeRating(AgeRating.valueOf(request.getAgeRating().name()));
            if (request.getStatus() != null) movie.setStatus(MovieStatus.valueOf(request.getStatus().name()));

            // Date
            if (request.getReleaseYear() != null) {
                movie.setReleaseDate(LocalDate.of(request.getReleaseYear(), 1, 1));
            }

            if (request.getCountryIds() != null) {
                List<Country> newCountries = countryRepository.findAllById(request.getCountryIds());

                if (movie.getCountries() == null) {
                    movie.setCountries(new HashSet<>());
                }
                movie.getCountries().clear();

                movie.getCountries().addAll(newCountries);
            }

            if (request.getGenreIds() != null) {
                List<Genre> newGenres = genreRepository.findAllById(request.getGenreIds());

                if (movie.getGenres() == null) {
                    movie.setGenres(new HashSet<>());
                }
                movie.getGenres().clear();
                movie.getGenres().addAll(newGenres);
            }

            if (request.getActorIds() != null) {
                List<Person> newActors = personRepository.findAllById(request.getActorIds());

                if (movie.getActors() == null) {
                    movie.setActors(new HashSet<>());
                }
                movie.getActors().clear();
                movie.getActors().addAll(newActors);
            }
            if (request.getDirectorIds() != null) {
                List<Person> newDirectors = personRepository.findAllById(request.getDirectorIds());

                if (movie.getDirectors() == null) {
                    movie.setDirectors(new HashSet<>());
                }
                movie.getDirectors().clear();
                movie.getDirectors().addAll(newDirectors);
            }

            // 3. Update Images (Nếu có file mới)
            if (request.getPosterImage() != null && !request.getPosterImage().isEmpty()) {
                String url = cloudinaryService.uploadImage(request.getPosterImage());
                movie.setPosterUrl(url);
            }
            if (request.getBackdropImage() != null && !request.getBackdropImage().isEmpty()) {
                String url = cloudinaryService.uploadImage(request.getBackdropImage());
                movie.setBackdropUrl(url);
            }
            User currentUser = getCurrentUser();
            if (currentUser != null) {
                movie.setUpdatedBy(currentUser);
            }

            Movie updatedMovie = movieRepository.save(movie);
            searchService.indexMovie(updatedMovie);
            return ServiceResult.Success().code(ErrorCode.SUCCESS).message("Câp nhật phim thành công");

        } catch (IOException e) {
            return ServiceResult.Failure().code(ErrorCode.FAILED).message("Lỗi khi tải ảnh lên: " + e.getMessage());
        }
    }
    @Transactional(readOnly = true)
    public ServiceResult getMovieInfo(UUID id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phim"));

        boolean isLoggedIn = isAuthenticated();

        List<Integer> genreIds = movie.getGenres().stream().map(Genre::getId).toList();
        Map<Integer, Long> genreCounts = new HashMap<>();
        if (!genreIds.isEmpty()) {
            List<Object[]> counts = genreRepository.countMoviesByGenreIds(genreIds);
            for (Object[] row : counts) {
                genreCounts.put((Integer) row[0], (Long) row[1]);
            }
        }

        List<MovieInfoResponse.CountryDto> countries = movie.getCountries().stream()
                .map(c -> MovieInfoResponse.CountryDto.builder().id(c.getId()).isoCode(c.getIsoCode()).name(c.getName()).build()).toList();

        List<MovieInfoResponse.GenreDto> genres = movie.getGenres().stream()
                .map(g -> MovieInfoResponse.GenreDto.builder().id(g.getId()).name(g.getName()).movieCount(genreCounts.getOrDefault(g.getId(), 0L)).build()).toList();

        List<MovieInfoResponse.PersonDto> actors = movie.getActors().stream()
                .map(p -> MovieInfoResponse.PersonDto.builder().id(p.getId()).name(p.getFullName()).avatar(p.getProfilePath()).build()).toList();

        List<MovieInfoResponse.PersonDto> directors = movie.getDirectors().stream()
                .map(p -> MovieInfoResponse.PersonDto.builder().id(p.getId()).name(p.getFullName()).avatar(p.getProfilePath()).build()).toList();

        MovieInfoResponse.PersonDto directorDto = movie.getDirectors().stream().findFirst()
                .map(p -> MovieInfoResponse.PersonDto.builder().id(p.getId()).name(p.getFullName()).avatar(p.getProfilePath()).build()).orElse(null);

        String movieSignedUrl = null;
        if (isLoggedIn && movie.getVideoUrl() != null && !movie.isSeries()) {

            String uid = movie.getVideoUrl();
            if (uid != null) {
                movieSignedUrl = cloudflareStreamService.generateSignedUrl(uid);
            }
        }

        List<MovieInfoResponse.SeasonDto> seasonDtos = new ArrayList<>();
        if (movie.isSeries()) {
            List<Season> seasons = seasonRepository.findAllByMovieIdWithEpisodes(movie.getId());

            seasons.sort(Comparator.comparingInt(Season::getSeasonNumber));

            boolean first = true;
            for (Season s : seasons) {
                if (first) {
                    List<MovieInfoResponse.EpisodeDto> eps = s.getEpisodes() == null ? List.of() :
                            s.getEpisodes().stream()
                                    .map(ep -> {
                                        String epSignedUrl = null;
                                        if (isLoggedIn && ep.getVideoUrl() != null) {
                                            String uid = ep.getVideoUrl();
                                            if (uid != null) {
                                                epSignedUrl = cloudflareStreamService.generateSignedUrl(uid);
                                            }
                                        }

                                        return MovieInfoResponse.EpisodeDto.builder()
                                                .id(ep.getId())
                                                .episodeNumber(ep.getEpisodeNumber())
                                                .title(ep.getTitle())
                                                .duration(ep.getDurationMin())
                                                .synopsis(ep.getSynopsis())
                                                .stillPath(ep.getStillPath())
                                                .airDate(ep.getAirDate())
                                                .videoUrl(epSignedUrl)
                                                .videoStatus(ep.getVideoStatus() != null ? String.valueOf(ep.getVideoStatus()) : "PENDING")
                                                .build();
                                    })
                                    .toList();

                    seasonDtos.add(MovieInfoResponse.SeasonDto.builder()
                            .id(s.getId())
                            .seasonNumber(s.getSeasonNumber())
                            .title(s.getTitle())
                            .episodes(eps)
                            .build());

                    first = false;
                } else {
                    seasonDtos.add(MovieInfoResponse.SeasonDto.builder()
                            .id(s.getId())
                            .seasonNumber(s.getSeasonNumber())
                            .title(s.getTitle())
                            .build());
                }
            }
        }

        // --- 6. BUILD RESPONSE ---
        MovieInfoResponse response = MovieInfoResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .originalTitle(movie.getOriginalTitle())
                .description(movie.getDescription())
                .releaseYear(movie.getReleaseDate() != null ? String.valueOf(movie.getReleaseDate().getYear()) : "N/A")
                .duration(movie.getDurationMin())
                .ageRating(String.valueOf(movie.getAgeRating()))
                .status(String.valueOf(movie.getStatus()))

                // [MỚI] Thêm Video Status cho Movie
                .videoStatus(movie.getVideoStatus() != null ? String.valueOf(movie.getVideoStatus()) : "PENDING")

                .countries(countries)
                .genres(genres)
                .director(directorDto)
                .actors(actors)
                .directors(directors)
                // [MỚI] Gán URL đã ký cho Movie (nếu là phim lẻ)
                .videoUrl(movieSignedUrl)
                .quality(movie.getQuality())
                .posterUrl(movie.getPosterUrl())
                .backdropUrl(movie.getBackdropUrl())
                .slug(movie.getSlug())
                .averageRating(movie.getAverageRating())
                .viewCount(movie.getViewCount())
                .trailerUrl(movie.getTrailerUrl())
                .isSeries(movie.isSeries())
                .seasons(seasonDtos)
                .build();

        return ServiceResult.Success()
                .code(ErrorCode.SUCCESS)
                .data(response);
    }

    public ServiceResult searchMovies(MovieSearchRequest request, Pageable pageable) {
        try {
            Specification<Movie> spec = createSearchSpec(request, null);
            Page<Movie> pageResult = movieRepository.findAll(spec, pageable);

            Page<MovieSearchResponse> pageDto = pageResult.map(movie -> {


                return MovieSearchResponse.builder()
                        .id(movie.getId())
                        .title(movie.getTitle())
                        .originalTitle(movie.getOriginalTitle())
                        .description(movie.getDescription())
                        .slug(movie.getSlug())

                        .posterUrl(movie.getPosterUrl())
                        .backdropUrl(movie.getBackdropUrl())
                        .videoUrl(movie.getVideoUrl())
                        .releaseDate(movie.getReleaseDate())
                        .releaseYear(movie.getReleaseDate() != null ? movie.getReleaseDate().getYear() : null)
                        .durationMin(movie.getDurationMin())
                        .ageRating(String.valueOf(movie.getAgeRating()))
                        .quality(movie.getQuality())
                        .status(String.valueOf(movie.getStatus()))
                        .isSeries(movie.isSeries())
                        .build();
            });

            return ServiceResult.Success()
                    .code(ErrorCode.SUCCESS)
                    .message("Tìm kiếm thành công")
                    .data(pageDto);

        } catch (Exception e) {
            // Log error here
            return ServiceResult.Failure()
                    .code(ErrorCode.FAILED)
                    .message("Lỗi hệ thống: " + e.getMessage());
        }
    }

    public ServiceResult searchMoviesPublic(MovieSearchRequest request, Pageable pageable) {
        try {
            Specification<Movie> spec = createSearchPublicSpec(request, null);
            Page<Movie> pageResult = movieRepository.findAll(spec, pageable);

            Page<MovieSearchResponse> pageDto = pageResult.map(movie -> {


                return MovieSearchResponse.builder()
                        .id(movie.getId())
                        .title(movie.getTitle())
                        .originalTitle(movie.getOriginalTitle())
                        .description(movie.getDescription())
                        .slug(movie.getSlug())

                        .posterUrl(movie.getPosterUrl())
                        .backdropUrl(movie.getBackdropUrl())
                        .videoUrl(movie.getVideoUrl())
                        .releaseDate(movie.getReleaseDate())
                        .releaseYear(movie.getReleaseDate() != null ? movie.getReleaseDate().getYear() : null)
                        .durationMin(movie.getDurationMin())
                        .ageRating(String.valueOf(movie.getAgeRating()))
                        .quality(movie.getQuality())
                        .status(String.valueOf(movie.getStatus()))
                        .isSeries(movie.isSeries())
                        .build();
            });

            return ServiceResult.Success()
                    .code(ErrorCode.SUCCESS)
                    .message("Tìm kiếm thành công")
                    .data(pageDto);

        } catch (Exception e) {
            // Log error here
            return ServiceResult.Failure()
                    .code(ErrorCode.FAILED)
                    .message("Lỗi hệ thống: " + e.getMessage());
        }
    }

    private Specification<Movie> createSearchPublicSpec(MovieSearchRequest request, UUID userId) {
        User user = getCurrentUser();

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(request.getQuery())) {
                String searchKey = "%" + request.getQuery().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), searchKey),
                        cb.like(cb.lower(root.get("originalTitle")), searchKey)
                ));
            }

            predicates.add(cb.equal(root.get("status"), "PUBLISHED"));

            if (request.getIsSeries() != null) {
                predicates.add(cb.equal(root.get("isSeries"), request.getIsSeries()));
            }

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            if (request.getAgeRating() != null) {
                predicates.add(cb.equal(root.get("ageRating"), request.getAgeRating())); // Hoặc .name()
            }

            if (request.getGenreIds() != null && !request.getGenreIds().isEmpty()) {
                Join<Movie, Genre> genreJoin = root.join("genres", JoinType.INNER);

                predicates.add(genreJoin.get("id").in(request.getGenreIds()));
            }

            if (request.getCountryIds() != null && !request.getCountryIds().isEmpty()) {
                Join<Movie, Country> countryJoin = root.join("countries", JoinType.INNER);
                predicates.add(countryJoin.get("id").in(request.getCountryIds()));
            }

            if (request.getReleaseYear() != null) {
                int year = request.getReleaseYear();
                LocalDate startOfYear = LocalDate.of(year, 1, 1);
                LocalDate endOfYear = LocalDate.of(year, 12, 31);

                predicates.add(cb.between(root.get("releaseDate"), startOfYear, endOfYear));
            }

            if (userId != null) {
                Subquery<UUID> subquery = query.subquery(UUID.class);
                Root<MovieLike> subRoot = subquery.from(MovieLike.class);

                subquery.select(subRoot.get("id").get("movieId"));
                subquery.where(cb.equal(subRoot.get("id").get("userId"), userId));

                predicates.add(root.get("id").in(subquery));
            }

            query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<Movie> createSearchSpec(MovieSearchRequest request, UUID userId) {
        User user = getCurrentUser();

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(request.getQuery())) {
                String searchKey = "%" + request.getQuery().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), searchKey),
                        cb.like(cb.lower(root.get("originalTitle")), searchKey)
                ));
            }

            if(  user != null && !user.getRole().getCode().equals("super_admin") ){
                predicates.add(cb.equal(root.get("status"), "PUBLISHED"));
            }

            if (request.getIsSeries() != null) {
                predicates.add(cb.equal(root.get("isSeries"), request.getIsSeries()));
            }

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            if (request.getAgeRating() != null) {
                predicates.add(cb.equal(root.get("ageRating"), request.getAgeRating())); // Hoặc .name()
            }

            if (request.getGenreIds() != null && !request.getGenreIds().isEmpty()) {
                Join<Movie, Genre> genreJoin = root.join("genres", JoinType.INNER);

                predicates.add(genreJoin.get("id").in(request.getGenreIds()));
            }

            if (request.getCountryIds() != null && !request.getCountryIds().isEmpty()) {
                Join<Movie, Country> countryJoin = root.join("countries", JoinType.INNER);
                predicates.add(countryJoin.get("id").in(request.getCountryIds()));
            }

            if (request.getReleaseYear() != null) {
                int year = request.getReleaseYear();
                LocalDate startOfYear = LocalDate.of(year, 1, 1);
                LocalDate endOfYear = LocalDate.of(year, 12, 31);

                predicates.add(cb.between(root.get("releaseDate"), startOfYear, endOfYear));
            }

            if (userId != null) {
                Subquery<UUID> subquery = query.subquery(UUID.class);
                Root<MovieLike> subRoot = subquery.from(MovieLike.class);

                subquery.select(subRoot.get("id").get("movieId"));
                subquery.where(cb.equal(subRoot.get("id").get("userId"), userId));

                predicates.add(root.get("id").in(subquery));
            }

            query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional
    public ServiceResult createMovie(MovieCreateRequest request) {
        try {
            Optional<Movie> existingMovie = movieRepository.findMovieByTitleIgnoreCase(request.getTitle());
            if( existingMovie.isPresent()){
                return ServiceResult.Failure()
                        .code(ErrorCode.FAILED)
                        .message("Phim với tiêu đề '" + request.getTitle() + "' đã tồn tại.");
            }
            String posterUrl = cloudinaryService.uploadImage(request.getPosterImage());
            String backdropUrl = cloudinaryService.uploadImage(request.getBackdropImage());

            Movie movie = Movie.builder()
                    .title(request.getTitle())
                    .originalTitle(request.getOriginalTitle())
                    .description(request.getDescription())
                    .durationMin(request.getDurationMin())
                    .isSeries(request.getIsSeries())
                    .ageRating(AgeRating.valueOf(request.getAgeRating().name()))
                    .status(MovieStatus.valueOf(request.getStatus().name()))
                    .posterUrl(posterUrl)
                    .backdropUrl(backdropUrl)
                    .slug(generateSlug(request.getTitle()))
                    .build();

            if (request.getReleaseYear() != null) {
                movie.setReleaseDate(LocalDate.of(request.getReleaseYear(), 1, 1));
            }

            // Genres
            if (request.getGenreIds() != null && !request.getGenreIds().isEmpty()) {
                List<Genre> genres = genreRepository.findAllById(request.getGenreIds());
                movie.setGenres(new HashSet<>(genres));
            }

            // Countries
            if (request.getCountryIds() != null && !request.getCountryIds().isEmpty()) {
                List<Country> countries = countryRepository.findAllById(request.getCountryIds());
                movie.setCountries(new HashSet<>(countries));
            }

            // Actors
            if (request.getActorIds() != null && !request.getActorIds().isEmpty()) {
                List<Person> actors = personRepository.findAllById(request.getActorIds());
                movie.setActors(new HashSet<>(actors));
            }

            if (request.getDirectorIds() != null && !request.getDirectorIds().isEmpty()) {
                List<Person> directors = personRepository.findAllById(request.getDirectorIds());
                movie.setDirectors(new HashSet<>(directors));
            }

            User currentUser = getCurrentUser();
            if (currentUser != null) {
                movie.setCreatedBy(currentUser);
                movie.setUpdatedBy(currentUser);
            }

            Movie savedMovie = movieRepository.save(movie);

            searchService.indexMovie(savedMovie);

            return ServiceResult.Success()
                    .code(ErrorCode.SUCCESS)
                    .data(savedMovie)
                    .message("Tạo phim thành công");


        } catch (IOException e) {
            return ServiceResult.Failure()
                    .code(ErrorCode.FAILED)
                    .message("Lỗi khi tải ảnh lên: " + e.getMessage());
        } catch (Exception e) {
            return ServiceResult.Failure()
                    .code(ErrorCode.FAILED)
                    .message("Lỗi hệ thống: " + e.getMessage());
        }
    }

    public ServiceResult getMostViewedMovies(Boolean isSeries) {
        Pageable pageable = PageRequest.of(0, 10);

        List<Movie> movies = movieRepository.findMostViewed(isSeries, pageable);

        List<MovieShortResponse> response = movies.stream().map(m -> MovieShortResponse.builder()
                        .id(m.getId())
                        .title(m.getTitle())
                        .originalTitle(m.getOriginalTitle())
                        .posterUrl(m.getPosterUrl())
                        .backdropUrl(m.getBackdropUrl())
                        .slug(m.getSlug())
                        .imdbScore(m.getImdbScore() != null ? m.getImdbScore().doubleValue() : 0.0)
                        .releaseYear(m.getReleaseDate() != null ? m.getReleaseDate().getYear() : null)
                        .build())
                .toList();

        return ServiceResult.Success().code(ErrorCode.SUCCESS).data(response);
    }

    private String generateSlug(String title) {
        return title.toLowerCase().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", "-")
                + "-" + System.currentTimeMillis();
    }


    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        UUID userId = null;

        if (principal instanceof UserPrincipal userPrincipal) {
            User u = userPrincipal.getUser();
            userId = u.getId();
        }

        if (userId != null) {
            return userRepository.findById(userId).orElse(null);
        }
        return null;
    }

    @Transactional(readOnly = true)
    public ServiceResult getMovieDetail(UUID id, String userIp) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phim"));
        boolean isLoggedIn = isAuthenticated();
        Map<UUID, Long> progressMap = new HashMap<>();
        User currentUser = getCurrentUser();
        if (isLoggedIn && currentUser != null) {
            List<ViewingHistory> histories = historyRepository.findByUserIdAndMovieId(currentUser.getId(), id);

            for (ViewingHistory h : histories) {
                if (h.getEpisode() != null) {
                    progressMap.put(h.getEpisode().getId(), h.getCurrentSecond());
                } else {

                    progressMap.put(null, h.getCurrentSecond());
                }
            }
        }

        List<UUID> actorIds = movie.getActors().stream().map(Person::getId).toList();
        List<UUID> directorIds = movie.getDirectors().stream().map(Person::getId).toList();

        Map<UUID, Long> actorCounts = getPersonMovieCounts(actorIds, true);
        Map<UUID, Long> directorCounts = getPersonMovieCounts(directorIds, false);

        List<PersonResponse> actorDtos = movie.getActors().stream()
                .map(p -> mapPersonToDto(p, actorCounts.getOrDefault(p.getId(), 0L)))
                .toList();

        List<PersonResponse> directorDtos = movie.getDirectors().stream()
                .map(p -> mapPersonToDto(p, directorCounts.getOrDefault(p.getId(), 0L)))
                .toList();

        List<MovieDetailResponse.SeasonDto> seasonDtos = null;
        if (movie.isSeries()) {
            List<Season> seasons = seasonRepository.findAllByMovieIdWithEpisodes(id);

            seasonDtos = seasons.stream().map(s -> MovieDetailResponse.SeasonDto.builder()
                    .id(s.getId())
                    .seasonNumber(s.getSeasonNumber())
                    .title(s.getTitle())
                    .episodes(s.getEpisodes() == null ? new ArrayList<>() : s.getEpisodes().stream()
                            .map(ep -> {

                                String signedEpisodeUrl = null;

                                if (isLoggedIn && ep.getVideoUrl() != null) {

                                    String uid = ep.getVideoUrl();

                                    if (uid != null) {
                                        signedEpisodeUrl = cloudflareStreamService.generateSignedUrl(uid);
                                    }
                                }

                                Long epProgress = progressMap.getOrDefault(ep.getId(), 0L);
                                return MovieDetailResponse.EpisodeDto.builder()
                                        .id(ep.getId())
                                        .episodeNumber(ep.getEpisodeNumber())
                                        .title(ep.getTitle())
                                        .durationMin(ep.getDurationMin())

                                        // Gán link đã ký vào đây
                                        .videoUrl(signedEpisodeUrl)
                                        .currentSecond(epProgress)
                                        .synopsis(ep.getSynopsis())
                                        .stillPath(ep.getStillPath())
                                        .airDate(ep.getAirDate())
                                        .build();
                            })
                            .toList())
                    .build()).toList();
        }

        List<GenreResponse> genreDtos = movie.getGenres().stream()
                .map(g -> new GenreResponse(g.getId(), g.getName(), 0L))
                .toList();

        List<CountryResponse> countryDtos = movie.getCountries().stream()
                .map(c -> new CountryResponse(c.getId(), c.getIsoCode(), c.getName()))
                .toList();

        long reviewCount = reviewRepository.countByMovieId(id);
        Double avgRating = reviewRepository.getAverageRatingByMovieId(id);

        boolean isLiked = false;

        if (currentUser != null) {
            isLiked = movieLikeRepository.existsById(new MovieLikeId(currentUser.getId(), id));
        }
        String signedUrl = null;
        if (isLoggedIn) {

            String uid = movie.getVideoUrl();

            signedUrl = cloudflareStreamService.generateSignedUrl(uid);
        }
        Long movieProgress = progressMap.getOrDefault(null, 0L);
        MovieDetailResponse response = MovieDetailResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .originalTitle(movie.getOriginalTitle())
                .description(movie.getDescription())
                .slug(movie.getSlug())

                .posterUrl(movie.getPosterUrl())
                .backdropUrl(movie.getBackdropUrl())
                .trailerUrl(movie.getTrailerUrl())
                .currentSecond(movieProgress)
                .releaseDate(movie.getReleaseDate())
                .releaseYear(movie.getReleaseDate() != null ? movie.getReleaseDate().getYear() : null)
                .durationMin(movie.getDurationMin())
                .ageRating(String.valueOf(movie.getAgeRating()))
                .quality(movie.getQuality())
                .status(String.valueOf(movie.getStatus()))
                .isSeries(movie.isSeries())
                .isLiked(isLiked)
                .averageRating(avgRating)
                .viewCount(movie.getViewCount())
                .reviewCount((int) reviewCount)
                .videoUrl(isLoggedIn ? signedUrl : null)
                .genres(genreDtos)
                .countries(countryDtos)
                .actors(actorDtos)
                .directors(directorDtos)
                .seasons(seasonDtos)
                .build();

        return ServiceResult.Success().data(response);
    }
    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuth = auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
        return isAuth;
    }
    // --- Helpers ---

    private Map<UUID, Long> getPersonMovieCounts(List<UUID> ids, boolean isActor) {
        Map<UUID, Long> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) return map;

        List<Object[]> results = isActor
                ? personRepository.countMoviesByActorIds(ids)
                : personRepository.countMoviesByDirectorIds(ids);

        for (Object[] row : results) {
            map.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }

    private PersonResponse mapPersonToDto(Person p, Long count) {
        return PersonResponse.builder()
                .id(p.getId())
                .fullName(p.getFullName())
                .job(p.getJob())
                .profilePath(p.getProfilePath())
                .movieCount(count)
                .build();
    }

    private ReviewResponse mapReviewToDto(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .userId(r.getUser().getId())
                .username(r.getUser().getUsername())
                .userAvatar(r.getUser().getAvatarUrl())
                .rating(r.getRating())
                .title(r.getTitle())
                .body(r.getBody())
                .createdAt(r.getCreatedAt())
                .build();
    }


    @Transactional
    public ServiceResult toggleLikeMovie(UUID movieId) {

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ServiceResult.Failure().code(401).message("Chưa đăng nhập");
        }

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phim"));

        MovieLikeId likeId = new MovieLikeId(currentUser.getId(), movie.getId());

        if (movieLikeRepository.existsById(likeId)) {
            movieLikeRepository.deleteById(likeId);
            return ServiceResult.Success().message("Gỡ thích phim thành công").data("UNLIKED");
        } else {
            MovieLike newLike = MovieLike.builder()
                    .id(likeId)
                    .user(currentUser)
                    .movie(movie)
                    .build();

            movieLikeRepository.save(newLike);
            return ServiceResult.Success().message("Đã thích phim").data("LIKED");
        }
    }


    private MovieSearchResponse mapToMovieSearchResponse(Movie movie) {
        return MovieSearchResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .originalTitle(movie.getOriginalTitle())
                .description(movie.getDescription())
                .slug(movie.getSlug())
                .posterUrl(movie.getPosterUrl())
                .backdropUrl(movie.getBackdropUrl())
                .releaseDate(movie.getReleaseDate())
                .releaseYear(movie.getReleaseDate() != null ? movie.getReleaseDate().getYear() : null)
                .durationMin(movie.getDurationMin())
                .ageRating(String.valueOf(movie.getAgeRating()))
                .quality(movie.getQuality())
                .status(String.valueOf(movie.getStatus()))
                .isSeries(movie.isSeries())
                .build();
    }

    public ServiceResult searchMovieUserLike(MovieSearchRequest request, Pageable pageable) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ServiceResult.Failure()
                        .code(ErrorCode.UNAUTHORIZED)
                        .message("Người dùng cần đăng nhập");
            }

            Specification<Movie> spec = createSearchSpec(request, currentUser.getId());

            Page<Movie> pageResult = movieRepository.findAll(spec, pageable);

            Page<MovieSearchResponse> pageDto = pageResult.map(this::mapToMovieSearchResponse);

            return ServiceResult.Success()
                    .code(ErrorCode.SUCCESS)
                    .message("Lấy danh sách phim yêu thích thành công")
                    .data(pageDto);

        } catch (Exception e) {
            e.printStackTrace();
            return ServiceResult.Failure()
                    .code(ErrorCode.FAILED)
                    .message("Lỗi hệ thống: " + e.getMessage());
        }
    }


    public ServiceResult checkAndSyncVideoStatus(String videoUid) {
        // 1. Gọi Cloudflare
        Map<String, Object> cfInfo = cloudflareService.getVideoDetails(videoUid);
        String state = (String) cfInfo.get("state");
        int height = (int) cfInfo.get("height");

        // 2. Nếu READY -> Update Database
        if ("ready".equalsIgnoreCase(state)) {
            String hlsUrl = String.format("https://customer-avv2h3ae3kvexdfh.cloudflarestream.com/%s/manifest/video.m3u8",
                    videoUid);
            String quality = determineQuality(height);

            boolean updated = false;

            // --- CASE 1: Check MOVIE ---
            Optional<Movie> movieOpt = movieRepository.findByVideoUrlContaining(videoUid);
            if (movieOpt.isPresent()) {
                Movie movie = movieOpt.get();
                if (movie.getVideoStatus() != VideoUploadStatus.READY) {
                    movie.setVideoUrl(videoUid);
                    movie.setVideoStatus(VideoUploadStatus.READY);
                    movie.setQuality(quality);
                    movie.setStatus(MovieStatus.PUBLISHED);
                    movieRepository.save(movie);
                    updated = true;
                }
            }
            // --- CASE 2: Check EPISODE (Nếu không phải Movie) ---
            else {
                Optional<Episode> episodeOpt = episodeRepository.findByVideoUrlContaining(videoUid);
                if (episodeOpt.isPresent()) {
                    Episode episode = episodeOpt.get();

                    if (!hlsUrl.equals(episode.getVideoUrl())) {
                        episode.setVideoUrl(videoUid);
                        episode.setVideoStatus(VideoUploadStatus.READY);
                        // episode.setQuality(quality); // Nếu episode có cột quality
                        episodeRepository.save(episode);
                        updated = true;
                    }
                }
            }

            if (updated) {
                log.info("✅ DB Updated for UID: {}", videoUid);
            }

            // Trả về URL stream để Frontend play ngay lập tức
            cfInfo.put("streamUrl", hlsUrl);
        }

        return ServiceResult.Success().data(cfInfo);
    }
    private String determineQuality(int height) {
        if (height >= 2160) return "4K";
        if (height >= 1440) return "2K";
        if (height >= 1080) return "1080P"; // Full HD
        if (height >= 720) return "720P";
        if (height >= 480) return "480P";
        return "240P"; // Hoặc Unknown
    }

    public ServiceResult searchWatchedMovies(MovieSearchRequest request, Pageable pageable) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ServiceResult.Failure()
                        .code(ErrorCode.UNAUTHORIZED)
                        .message("Người dùng cần đăng nhập");
            }

            Specification<Movie> spec = (root, query, cb) -> {
                // Base predicates from search request
                Predicate base = createSearchSpec(request, null).toPredicate(root, query, cb);

                // Subquery to filter movies that have viewing history by current user
                Subquery<UUID> subquery = query.subquery(UUID.class);
                Root<ViewingHistory> vh = subquery.from(ViewingHistory.class);
                subquery.select(vh.get("movie").get("id"));
                subquery.where(cb.equal(vh.get("user").get("id"), currentUser.getId()));

                Predicate watchedByUser = root.get("id").in(subquery);

                query.distinct(true);
                return cb.and(base, watchedByUser);
            };

            Page<Movie> pageResult = movieRepository.findAll(spec, pageable);
            Page<MovieSearchResponse> pageDto = pageResult.map(this::mapToMovieSearchResponse);

            return ServiceResult.Success()
                    .code(ErrorCode.SUCCESS)
                    .message("Lấy danh sách phim đã xem thành công")
                    .data(pageDto);
        } catch (Exception e) {
            e.printStackTrace();
            return ServiceResult.Failure()
                    .code(ErrorCode.FAILED)
                    .message("Lỗi hệ thống: " + e.getMessage());
        }
    }
}

