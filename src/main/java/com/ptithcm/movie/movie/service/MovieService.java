package com.ptithcm.movie.movie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptithcm.movie.common.constant.MovieStatus;
import com.ptithcm.movie.common.constant.PersonJob;
import com.ptithcm.movie.common.dto.PagedResponseDto;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.external.cloudinary.CloudinaryService;
import com.ptithcm.movie.external.tmdb.TMDbService;
import com.ptithcm.movie.external.tmdb.dto.TvSeasonDetailDto;
import com.ptithcm.movie.movie.dto.*;
import com.ptithcm.movie.movie.entity.*;
import com.ptithcm.movie.movie.repository.*;
import com.ptithcm.movie.user.entity.User;
import com.ptithcm.movie.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@Service
@RequiredArgsConstructor
public class MovieService {
    private final MovieRepository movieRepository;
    private final PeopleRepository peopleRepository;
    private final GenreRepository genreRepository;
    private final CountryRepository countryRepository;
    private final SeasonRepository seasonRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final EpisodeRepository episodeRepository;
    private final TMDbService tmdbService;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public ServiceResult updateMovie(
            UUID movieId, // ID của Movie cần sửa
            MovieRequestDto dto, // Dữ liệu mới
            MultipartFile posterFile,
            MultipartFile backdropFile,
            UserDetails currentUser
    ) {

        try {
            // 1. Tìm Movie Entity
            Movie movie = movieRepository.findById(movieId)
                    .orElseThrow(() -> new RuntimeException("Movie not found"));


            Set<Genre> genres = findOrCreateGenres(dto.getGenres());
            Set<Country> countries = findOrCreateCountries(dto.getCountries());
            Set<Person> actors = findOrCreatePeople(dto.getActors(), PersonJob.ACTOR);
            Set<Person> directors = new HashSet<>();
            if (dto.getDirector() != null) {
                directors.add(findOrCreatePerson(dto.getDirector(), PersonJob.DIRECTOR));
            }

            // 4. Cập nhật các trường đơn giản
            movie.setTitle(dto.getTitle());
            movie.setDescription(dto.getDescription());
            movie.setImdbId(dto.getImdbId() != null && !dto.getImdbId().isBlank() ? dto.getImdbId() : null);
            movie.setSlug(slugify(dto.getTitle())); // (Có thể cần logic check trùng slug)

            if (dto.getRelease() != null && !dto.getRelease().isEmpty()) {
                movie.setReleaseDate(LocalDate.of(Integer.parseInt(dto.getRelease()), 1, 1));
            }

            movie.setDurationMin(dto.getDuration());
            movie.setAgeRating(dto.getAge());
            movie.setStatus(dto.getStatus());

            if (dto.getTrailerUrl() != null && !dto.getTrailerUrl().isBlank()) {
                movie.setTrailerUrl("https://www.youtube.com/watch?v=" + dto.getTrailerUrl());
            }

            // 5. (CẬP NHẬT) Upload ảnh MỚI (nếu có)
            Integer tmdbId = movie.getTmdbId(); // Lấy tmdbId từ movie đã lưu

            // 5a. Upload Poster (Ưu tiên File)
            if (posterFile != null && !posterFile.isEmpty()) {
                String publicId = "streamify/movies/posters/" + (tmdbId != null ? tmdbId : movie.getId());
                movie.setPosterUrl(cloudinaryService.uploadFile(posterFile, publicId));
            } else if (dto.getPoster() != null && !dto.getPoster().isBlank()) {
                // (Giữ URL từ DTO - có thể là URL cũ)
                movie.setPosterUrl(dto.getPoster());
            }

            // 5b. Upload Backdrop (Ưu tiên File)
            if (backdropFile != null && !backdropFile.isEmpty()) {
                String publicId = "streamify/movies/backdrops/" + (tmdbId != null ? tmdbId : movie.getId());
                movie.setBackdropUrl(cloudinaryService.uploadFile(backdropFile, publicId));
            } else if (dto.getBackdrop() != null && !dto.getBackdrop().isBlank()) {
                movie.setBackdropUrl(dto.getBackdrop());
            }

            // 6. Gắn các quan hệ (Clear và Add lại)
            movie.getGenres().clear();
            movie.getGenres().addAll(genres);

            movie.getCountries().clear();
            movie.getCountries().addAll(countries);

            movie.getActors().clear();
            movie.getActors().addAll(actors);

            movie.getDirectors().clear();
            movie.getDirectors().addAll(directors);

            // 7. Lưu Movie (Không cần save(), vì @Transactional
            //    nhưng 'save' rõ ràng cũng tốt)
            Movie updatedMovie = movieRepository.save(movie);

            // (LƯU Ý: Chúng ta BỎ QUA dto.getSeasons() ở đây)

            return ServiceResult.Success()
                    .message("Content updated successfully")
                    .data(updatedMovie);

        } catch (Exception e) {
            return ServiceResult.Failure()
                    .message("Failed to update content: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true) // (Dùng readOnly=true cho API GET)
    public ServiceResult getMovieById(UUID id) {
        try {
            // 1. Lấy Movie và các quan hệ (Ngoại trừ Seasons)
            Movie movie = movieRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new RuntimeException("Movie not found with ID: " + id));

            // 2. (Mới) Lấy Seasons VÀ Episodes (nếu là TV)
            if (Boolean.TRUE.equals(movie.getIsSeries())) {
                // (Đây là cách fetch riêng để tránh lỗi "multiple bag")
                List<Season> seasons = seasonRepository.findByMovieIdWithEpisodes(id);
                movie.setSeasons(seasons);
            }

            // 3. Chuyển đổi Entity sang DTO
            MovieDetailResponseDto responseDto = mapMovieToDetailDto(movie);

            return ServiceResult.Success()
                    .message("Movie details fetched successfully.")
                    .data(responseDto);

        } catch (Exception e) {
            return ServiceResult.Failure()
                    .message("Failed to fetch movie details: " + e.getMessage());
        }
    }

    private MovieDetailResponseDto mapMovieToDetailDto(Movie movie) {
        MovieDetailResponseDto dto = new MovieDetailResponseDto();
        dto.setId(movie.getTmdbId());
        dto.setTitle(movie.getTitle());
        dto.setOriginal_name(movie.getOriginalTitle());
        dto.setOverview(movie.getDescription());
        dto.setPoster_path(movie.getPosterUrl()); // (Frontend sẽ dùng URL Cloudinary)
        dto.setBackdrop_path(movie.getBackdropUrl());
        dto.setRelease_date(movie.getReleaseDate() != null ? movie.getReleaseDate().toString() : null);
        dto.setRuntime(movie.getDurationMin());
        dto.setStatus(movie.getStatus() != null ? movie.getStatus().name() : null);
        dto.setTrailer_key(movie.getTrailerUrl()); // (Đây là URL đầy đủ)
        dto.setImdb_id(movie.getImdbId());
        dto.setSeries(movie.getIsSeries());

        // Map các quan hệ (List/Set)
        dto.setGenres(movie.getGenres().stream()
                .map(MovieDetailResponseDto.PublicGenreDto::new)
                .collect(Collectors.toList()));

        dto.setProduction_countries(movie.getCountries().stream()
                .map(MovieDetailResponseDto.PublicCountryDto::new)
                .collect(Collectors.toList()));

        dto.setCast(movie.getActors().stream()
                .map(MovieDetailResponseDto.PublicPersonDto::new)
                .collect(Collectors.toList()));

        // Xử lý Director/Creator
        if (movie.getDirectors() != null && !movie.getDirectors().isEmpty()) {
            Person director = movie.getDirectors().iterator().next(); // Lấy người đầu tiên
            dto.setDirector(new MovieDetailResponseDto.PublicPersonDto(director));

            // (Nếu là TV, 'created_by' có thể là list)
            if (Boolean.TRUE.equals(movie.getIsSeries())) {
                dto.setCreated_by(movie.getDirectors().stream()
                        .map(MovieDetailResponseDto.PublicPersonDto::new)
                        .collect(Collectors.toList()));
            }
        }

        // Xử lý Seasons (nếu là TV)
        if (Boolean.TRUE.equals(movie.getIsSeries()) && movie.getSeasons() != null) {
            dto.setSeasons(movie.getSeasons().stream()
                    .map(MovieDetailResponseDto.PublicSeasonDto::new)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public PagedResponseDto<MovieItemDto> getMovies(
            String query,
            MovieStatus status,
            Boolean isSeries,
            int page,
            int size
    ) {
        // 1. Tạo Pageable (Không đổi)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // 2. (CẬP NHẬT) Xây dựng Specification

        // 2a. Lấy các spec con (có thể là null)
        Specification<Movie> titleSpec = MovieRepository.titleContains(query);
        Specification<Movie> statusSpec = MovieRepository.hasStatus(status);
        Specification<Movie> seriesSpec = MovieRepository.isSeries(isSeries);

        // 2b. Khởi tạo 'spec' là null
        Specification<Movie> spec = null;

        // 2c. Nối (chain) các điều kiện nếu chúng tồn tại
        if (titleSpec != null) {
            spec = titleSpec; // Bắt đầu chuỗi
        }

        if (statusSpec != null) {
            // Nếu spec vẫn là null -> gán. Nếu không -> .and()
            spec = (spec == null) ? statusSpec : spec.and(statusSpec);
        }

        if (seriesSpec != null) {
            spec = (spec == null) ? seriesSpec : spec.and(seriesSpec);
        }

        // 3. Gọi Repository
        // JpaSpecificationExecutor.findAll(spec, pageable)
        // xử lý chính xác trường hợp 'spec' là 'null' (sẽ trả về tất cả)
        Page<Movie> moviePage = movieRepository.findAll(spec, pageable);

        // 4. Biến đổi (Không đổi)
        Page<MovieItemDto> dtoPage = moviePage.map(MovieItemDto::new);

        // 5. Trả về (Không đổi)
        return new PagedResponseDto<>(dtoPage);
    }
    /**
     * API chính: Thêm Movie hoặc TV Series mới
     * Sử dụng @Transactional để đảm bảo tất cả các thao tác (save, find-or-create)
     * cùng thành công hoặc cùng thất bại (rollback).
     */
    @Transactional(rollbackFor = Exception.class)
    public ServiceResult createMovie(
            MovieRequestDto dto,
            MultipartFile posterFile,
            MultipartFile backdropFile,
            UserDetails currentUser
    ) throws IOException {


        // 1. Lấy User (created_by)
        User user = userRepository.findByEmailIgnoreCase(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Xử lý "Find-or-Create" cho các quan hệ
        Set<Genre> genres = findOrCreateGenres(dto.getGenres());
        Set<Country> countries = findOrCreateCountries(dto.getCountries());
        Set<Person> actors = findOrCreatePeople(dto.getActors(), PersonJob.ACTOR);
        Set<Person> directors = new HashSet<>();
        if (dto.getDirector() != null) {
            directors.add(findOrCreatePerson(dto.getDirector(), PersonJob.DIRECTOR));
        }

        // 3. Tạo Entity Movie
        Movie movie = new Movie();
        movie.setTitle(dto.getTitle());
        movie.setDescription(dto.getDescription());
        movie.setTmdbId(dto.getTmdbId());
        if (dto.getImdbId() != null && !dto.getImdbId().isBlank()) {
            movie.setImdbId(dto.getImdbId());
        } else {
            movie.setImdbId(null);
        }
        movie.setIsSeries(dto.getIsSeries());
        movie.setSlug(slugify(dto.getTitle()));

        if (dto.getRelease() != null && !dto.getRelease().isEmpty()) {
            try {
                movie.setReleaseDate(LocalDate.of(Integer.parseInt(dto.getRelease()), 1, 1));
            } catch (NumberFormatException e) {
                // (Bạn có thể log lỗi ở đây nếu cần)
            }
        }

        movie.setDurationMin(dto.getDuration());
        movie.setAgeRating(dto.getAge());
        movie.setStatus(dto.getStatus());
        movie.setPosterUrl(dto.getPoster());
        movie.setBackdropUrl(dto.getBackdrop());

        Integer tmdbId = dto.getTmdbId();

        // 3a. Upload Poster
        String posterUrl = null;
        if (posterFile != null && !posterFile.isEmpty()) {
            // (Ưu tiên 1: Người dùng upload file)
            String publicId = "streamify/movies/posters/" + (tmdbId != null ? tmdbId : UUID.randomUUID());
            posterUrl = cloudinaryService.uploadFile(posterFile, publicId);
        } else if (dto.getPoster() != null && !dto.getPoster().isBlank() && tmdbId != null) {
            // (Ưu tiên 2: Dùng URL auto-fill từ TMDb)
            String publicId = "streamify/movies/posters/" + tmdbId;
            posterUrl = cloudinaryService.uploadFromUrl(dto.getPoster(), publicId);
        }
        movie.setPosterUrl(posterUrl);

        // 3b. Upload Backdrop
        String backdropUrl = null;
        if (backdropFile != null && !backdropFile.isEmpty()) {
            // (Ưu tiên 1: Người dùng upload file)
            String publicId = "streamify/movies/backdrops/" + (tmdbId != null ? tmdbId : UUID.randomUUID());
            backdropUrl = cloudinaryService.uploadFile(backdropFile, publicId);
        } else if (dto.getBackdrop() != null && !dto.getBackdrop().isBlank() && tmdbId != null) {
            // (Ưu tiên 2: Dùng URL auto-fill từ TMDb)
            String publicId = "streamify/movies/backdrops/" + tmdbId;
            backdropUrl = cloudinaryService.uploadFromUrl(dto.getBackdrop(), publicId);
        }
        movie.setBackdropUrl(backdropUrl);

        if (dto.getTrailerUrl() != null && !dto.getTrailerUrl().isBlank()) {
            movie.setTrailerUrl("https://www.youtube.com/watch?v=" + dto.getTrailerUrl());
        }

        // 4. Gắn các quan hệ
        movie.setCreatedBy(user);
        movie.setGenres(genres);
        movie.setCountries(countries);
        movie.setActors(actors);
        movie.setDirectors(directors);

        // 5. Lưu Movie (Lấy ID)
        Movie savedMovie = movieRepository.save(movie);

        // --- (CẬP NHẬT LOGIC LƯU SEASONS VÀ EPISODES) ---
        if (Boolean.TRUE.equals(dto.getIsSeries()) && dto.getSeasons() != null) {

            // Lặp qua TỪNG season DTO
            for (SeasonDto seasonDto : dto.getSeasons()) {

                // 6a. Lưu Season (để lấy UUID)
                Season season = new Season();
                season.setMovie(savedMovie);
                season.setSeasonNumber(seasonDto.getSeason_number());
                season.setTitle(seasonDto.getName());
                season.setTmdbId(seasonDto.getId());
                Season savedSeason = seasonRepository.save(season); // LƯU

                // 6b. (MỚI) Tự động gọi TMDb để lấy Episodes cho season này
                // (Bỏ qua "Specials" (season 0) nếu không cần)
                if (seasonDto.getSeason_number() > 0) {
                    TvSeasonDetailDto seasonDetail = tmdbService.getTvSeasonDetails(
                            savedMovie.getTmdbId(),
                            seasonDto.getSeason_number()
                    );

                    // 6c. (MỚI) Tạo list Episodes
                    List<Episode> episodes = seasonDetail.episodes().stream()
                            .map(epDto -> {
                                Episode episode = new Episode();
                                episode.setSeason(savedSeason); // <-- Link với Season UUID
                                episode.setEpisodeNumber(epDto.episodeNumber());
                                episode.setTitle(epDto.name());
                                episode.setSynopsis(epDto.overview());
                                episode.setDurationMin(epDto.runtime());
                                episode.setTmdbId(epDto.tmdbId());
                                episode.setStillPath(epDto.stillPath());

                                // Parse air_date (xử lý an toàn)
                                try {
                                    if (epDto.airDate() != null) {
                                        episode.setAirDate(LocalDate.parse(epDto.airDate()));
                                    }
                                } catch (DateTimeParseException e) {
                                    // Bỏ qua nếu định dạng ngày sai
                                }

                                return episode;
                            }).collect(Collectors.toList());

                    // 6d. (MỚI) Lưu Episodes vào DB
                    episodeRepository.saveAll(episodes);
                }
            }
        }

        // 7. (CẬP NHẬT) Trả về Success
        return ServiceResult.Success()
                .message("Content added successfully");

    }

    // --- Helpers (Find-or-Create) ---

    private Set<Genre> findOrCreateGenres(List<GenreDto> genreDtos) {
        Set<Genre> genres = new HashSet<>();
        if (genreDtos == null) return genres;

        for (GenreDto dto : genreDtos) {
            // Tìm bằng tmdb_id
            Genre g = genreRepository.findByTmdbId(dto.getTmdb_id())
                    .orElseGet(() -> {
                        // Nếu không thấy, tạo mới
                        Genre newGenre = new Genre();
                        newGenre.setTmdbId(dto.getTmdb_id());
                        newGenre.setName(dto.getName());
                        return genreRepository.save(newGenre);
                    });
            genres.add(g);
        }
        return genres;
    }

    private Set<Country> findOrCreateCountries(List<CountryDto> countryDtos) {
        Set<Country> countries = new HashSet<>();
        if (countryDtos == null) return countries;

        for (CountryDto dto : countryDtos) {
            countries.add(findOrCreateCountry(dto)); // Gọi hàm helper (số ít)
        }
        return countries;
    }

    private Country findOrCreateCountry(CountryDto dto) {
        if (dto == null) return null;

        // Tìm bằng iso_code
        return countryRepository.findByIsoCode(dto.getIso_code())
                .orElseGet(() -> {
                    // Nếu không thấy, tạo mới
                    Country newCountry = new Country();
                    newCountry.setIsoCode(dto.getIso_code());
                    newCountry.setName(dto.getName());
                    return countryRepository.save(newCountry);
                });
    }

    private Set<Person> findOrCreatePeople(List<PersonDto> personDtos, PersonJob job) {
        Set<Person> people = new HashSet<>();
        if (personDtos == null) return people;

        for (PersonDto dto : personDtos) {
            people.add(findOrCreatePerson(dto, job));
        }
        return people;
    }

    private Person findOrCreatePerson(PersonDto dto, PersonJob job) {
        if (dto == null) return null;

        // Tìm bằng tmdb_id
        return peopleRepository.findByTmdbId(dto.getId()) // 'id' trong DTO là 'tmdb_id'
                .orElseGet(() -> {
                    // Nếu không thấy, tạo mới
                    Person newPerson = new Person();
                    newPerson.setTmdbId(dto.getId());
                    newPerson.setFullName(dto.getName());
                    newPerson.setProfilePath(dto.getImg());
                    newPerson.setJob(job);
                    // (Bạn có thể gọi API TMDb /person/{id} để lấy thêm
                    //  biography, birth_date... nhưng sẽ chậm)
                    return peopleRepository.save(newPerson);
                });
    }

    // --- Helper (Tạo Slug) ---

    private String slugify(String text) {
        String slugBase;

        // 1. Xử lý trường hợp đầu vào rỗng hoặc null
        if (text == null || text.isEmpty()) {
            slugBase = "movie"; // Dùng "movie" làm cơ sở
        } else {
            // 2. Tạo slug từ text (giữ nguyên logic của bạn)
            String slug = text.toLowerCase()
                    .replaceAll("\\s+", "-") // Thay khoảng trắng
                    .replaceAll("[^a-z0-9-]", "") // Xóa ký tự đặc biệt
                    .replaceAll("-+", "-") // Xóa gạch nối kép
                    .replaceAll("^-|-$", ""); // Xóa gạch nối đầu/cuối

            // 3. Kiểm tra xem slug có bị rỗng không (ví dụ: text = "!!!")
            slugBase = slug.isEmpty() ? "movie" : slug;
        }

        // 4. Tạo chuỗi ngẫu nhiên
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);

        // 5. Trả về kết quả cuối cùng: "text-title-chuoirandom"
        return slugBase + "-" + randomSuffix;
    }

}
