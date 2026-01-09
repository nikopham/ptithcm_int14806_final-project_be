package com.ptithcm.movie.movie.dto.request;

import com.ptithcm.movie.common.constant.AgeRating;
import com.ptithcm.movie.common.constant.MovieStatus;
import lombok.Data;

import java.util.List;

@Data
public class MovieSearchRequest {
    private String query;       // Tìm theo title (hoặc original_title)
    private Boolean isSeries;   // Lọc phim lẻ hay phim bộ
    private AgeRating ageRating; // Lọc theo độ tuổi
    private MovieStatus status;  // Lọc trạng thái (VD: chỉ lấy PUBLISHED)
    private List<Integer> genreIds;   // Thay vì Integer genreId
    private List<Integer> countryIds;
    private Integer releaseYear;
}