package com.ptithcm.movie.movie.service;

import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.movie.entity.Genre;
import com.ptithcm.movie.movie.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
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
}
