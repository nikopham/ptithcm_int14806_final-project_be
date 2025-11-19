package com.ptithcm.movie.movie.service;

import com.ptithcm.movie.common.dto.PagedResponseDto;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.movie.dto.CreateGenreRequest;
import com.ptithcm.movie.movie.dto.GenreItemDto;
import com.ptithcm.movie.movie.dto.UpdateGenreRequest;
import com.ptithcm.movie.movie.entity.Genre;
import com.ptithcm.movie.movie.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenreService {
    private final GenreRepository genreRepository;

    /**
     * Logic nghiệp vụ để lấy tất cả Genres
     */
    public ServiceResult getAllGenres() {
        try {
            List<Genre> genres = genreRepository.findAll();
            return ServiceResult.Success()
                    .message("Genres fetched successfully")
                    .data(genres); // <-- 2. Đặt data vào
        } catch (Exception e) {
            return ServiceResult.Failure()
                    .message("Failed to fetch genres: " + e.getMessage());
        }
    }

    public ServiceResult getGenresPaginated(String query, int page, int size) {
        try {
            // 1. Tạo Pageable (Mặc định sắp xếp theo ID hoặc Tên)
            // Bạn có thể thêm Sort nếu muốn
            Pageable pageable = PageRequest.of(page, size);

            // 2. Gọi Repository
            Page<GenreItemDto> genrePage = genreRepository.searchGenresWithCount(query, pageable);

            // 3. Đóng gói vào PagedResponseDto (Bạn đã có class này từ bài trước)
            PagedResponseDto<GenreItemDto> pagedData = new PagedResponseDto<>(genrePage);

            return ServiceResult.Success()
                    .message("Genres fetched successfully")
                    .data(pagedData);

        } catch (Exception e) {
            return ServiceResult.Failure()
                    .message("Failed to fetch genres: " + e.getMessage());
        }
    }

    public ServiceResult createGenre(CreateGenreRequest request) {
        try {
            // 1. Validate Tên (Bắt buộc)
            if (request.getName() == null || request.getName().isBlank()) {
                return ServiceResult.Failure().message("Genre name is required.");
            }

            // 2. Kiểm tra trùng tên (Trong DB)
            if (genreRepository.existsByNameIgnoreCase(request.getName().trim())) {
                return ServiceResult.Failure().message("Genre '" + request.getName() + "' already exists.");
            }

            // 3. Kiểm tra trùng TMDb ID (Nếu có gửi lên)
            if (request.getTmdbId() != null && genreRepository.existsByTmdbId(request.getTmdbId())) {
                return ServiceResult.Failure().message("Genre with this TMDb ID already exists.");
            }

            // 4. Tạo và Lưu
            Genre genre = new Genre();
            genre.setName(request.getName().trim());
            genre.setTmdbId(request.getTmdbId()); // Có thể là null

            Genre savedGenre = genreRepository.save(genre);

            return ServiceResult.Success()
                    .message("Genre created successfully")
                    .data(savedGenre);

        } catch (Exception e) {
            return ServiceResult.Failure()
                    .message("Failed to create genre: " + e.getMessage());
        }
    }

    public ServiceResult updateGenre(Integer id, UpdateGenreRequest request) {
        try {
            // 1. Tìm Genre cần sửa
            Genre genre = genreRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Genre not found with id: " + id));

            // 2. Validate Tên (Bắt buộc)
            if (request.getName() == null || request.getName().isBlank()) {
                return ServiceResult.Failure().message("Genre name is required.");
            }

            // 3. Kiểm tra trùng tên (Trừ chính nó)
            if (genreRepository.existsByNameIgnoreCaseAndIdNot(request.getName().trim(), id)) {
                return ServiceResult.Failure().message("Genre name '" + request.getName() + "' is already taken.");
            }

            // 4. Kiểm tra trùng TMDb ID (Trừ chính nó)
            if (request.getTmdbId() != null &&
                    genreRepository.existsByTmdbIdAndIdNot(request.getTmdbId(), id)) {
                return ServiceResult.Failure().message("Genre with this TMDb ID already exists.");
            }

            // 5. Cập nhật thông tin
            genre.setName(request.getName().trim());
            genre.setTmdbId(request.getTmdbId());

            // 6. Lưu
            Genre updatedGenre = genreRepository.save(genre);

            return ServiceResult.Success()
                    .message("Genre updated successfully")
                    .data(updatedGenre);

        } catch (Exception e) {
            return ServiceResult.Failure()
                    .message("Failed to update genre: " + e.getMessage());
        }
    }
}
