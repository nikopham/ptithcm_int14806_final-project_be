package com.ptithcm.movie.movie.service;

import com.ptithcm.movie.common.constant.ErrorCode;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.movie.dto.request.GenreRequest;
import com.ptithcm.movie.movie.dto.request.GenreSearchRequest;
import com.ptithcm.movie.movie.dto.response.GenreResponse;
import com.ptithcm.movie.movie.dto.response.GenreWithMoviesResponse;
import com.ptithcm.movie.movie.dto.response.MovieShortResponse;
import com.ptithcm.movie.movie.entity.Genre;
import com.ptithcm.movie.movie.entity.Movie;
import com.ptithcm.movie.movie.repository.GenreRepository;
import com.ptithcm.movie.movie.repository.MovieRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository genreRepository;
    private final MovieRepository movieRepository;

    public ServiceResult getPublishedGenres() {
        List<Genre> genres = genreRepository.findAllWithPublishedMovies();
        List<GenreResponse> response = genres.stream()
                .map(g -> new GenreResponse(g.getId(), g.getName(), 0L))
                .toList();
        return ServiceResult.Success().data(response);
    }

    public ServiceResult searchGenres(GenreSearchRequest request, Pageable pageable) {
        try {
            Specification<Genre> spec = createGenreSpec(request);
            Page<Genre> genrePage = genreRepository.findAll(spec, pageable);

            List<Integer> genreIds = genrePage.getContent().stream()
                    .map(Genre::getId)
                    .toList();

            Map<Integer, Long> countMap = new HashMap<>();
            if (!genreIds.isEmpty()) {
                List<Object[]> counts = genreRepository.countMoviesByGenreIds(genreIds);
                for (Object[] row : counts) {
                    countMap.put((Integer) row[0], (Long) row[1]);
                }
            }

            Page<GenreResponse> responsePage = genrePage.map(genre -> {
                return GenreResponse.builder()
                        .id(genre.getId())
                        .name(genre.getName())
                        .movieCount(countMap.getOrDefault(genre.getId(), 0L))
                        .build();
            });

            return ServiceResult.Success()
                    .code(ErrorCode.SUCCESS)
                    .message("Tìm kiếm thể loại phim thành công")
                    .data(responsePage);

        } catch (Exception e) {
            return ServiceResult.Failure()
                    .code(ErrorCode.FAILED)
                    .message("Lỗi khi tìm kiếm thể loại phim: " + e.getMessage());
        }
    }

    public ServiceResult getFeaturedGenresWithMovies(Boolean isSeries) {
        List<Genre> topGenres = genreRepository.findTopGenresByLatestMovies(
                isSeries,
                PageRequest.of(0, 15)
        );

        List<GenreWithMoviesResponse> response = topGenres.stream().map(genre -> {

            List<Movie> movies = movieRepository.findLatestByGenre(
                    genre.getId(),
                    isSeries,
                    PageRequest.of(0, 6)
            );

            List<MovieShortResponse> movieDtos = movies.stream().map(m -> MovieShortResponse.builder()
                    .id(m.getId())
                    .title(m.getTitle())
                    .originalTitle(m.getOriginalTitle())
                    .posterUrl(m.getPosterUrl())
                    .slug(m.getSlug())
                    .imdbScore(m.getImdbScore() != null ? m.getImdbScore().doubleValue() : 0.0)
                    .releaseYear(m.getReleaseDate() != null ? m.getReleaseDate().getYear() : null)
                    .build()).toList();

            return GenreWithMoviesResponse.builder()
                    .genreId(genre.getId())
                    .genreName(genre.getName())
                    .movies(movieDtos)
                    .build();
        }).toList();

        return ServiceResult.Success().code(ErrorCode.SUCCESS).data(response);
    }

    public ServiceResult getAllGenres() {
        List<Genre> genres = genreRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));

        List<GenreResponse> response = genres.stream()
                .map(g -> GenreResponse.builder()
                        .id(g.getId())
                        .name(g.getName())
                        .build())
                .toList();

        return ServiceResult.Success().code(ErrorCode.SUCCESS).data(response);
    }

    private Specification<Genre> createGenreSpec(GenreSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(request.getQuery())) {
                String searchKey = "%" + request.getQuery().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), searchKey));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional
    public ServiceResult createGenre(GenreRequest request) {
        if (genreRepository.existsByNameIgnoreCase(request.getName().trim())) {
            return ServiceResult.Failure()
                    .code(ErrorCode.FAILED)
                    .message("Thể loại phim với tên '" + request.getName() + "' đã tồn tại");
        }

        Genre genre = Genre.builder()
                .name(request.getName().trim())
                .build();

        Genre savedGenre = genreRepository.save(genre);

        return ServiceResult.Success().code(ErrorCode.SUCCESS)
                .message("Tạo thể loại phim thành công")
                .data(savedGenre);
    }

    @Transactional
    public ServiceResult updateGenre(Integer id, GenreRequest request) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thể loại phim"));

        String newName = request.getName().trim();

        if (genreRepository.existsByNameIgnoreCaseAndIdNot(newName, id)) {
            return ServiceResult.Failure()
                    .code(ErrorCode.FAILED)
                    .message("Thể loại phim '" + newName + "' đã tồn tại");
        }

        genre.setName(newName);
        Genre updatedGenre = genreRepository.save(genre);

        return ServiceResult.Success().code(ErrorCode.SUCCESS)
                .message("Cập nhật thể loại phim thành công")
                .data(updatedGenre);
    }

    @Transactional
    public ServiceResult deleteGenre(Integer id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thể loại phim"));

        genreRepository.deleteMovieGenreRelations(id);

        genreRepository.delete(genre);

        return ServiceResult.Success().code(ErrorCode.SUCCESS)
                .message("Xóa thể loại phim thành công");
    }
}