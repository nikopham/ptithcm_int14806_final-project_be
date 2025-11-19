package com.ptithcm.movie.movie.dto;


import com.ptithcm.movie.common.constant.AgeRating;
import com.ptithcm.movie.common.constant.MovieStatus;
import lombok.Data; // (Hoặc tự viết Getter/Setter)
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Data
@Getter
@Setter
public class MovieRequestDto {

    private Integer tmdbId;
    private String imdbId;
    private String title;
    private String description;
    private String release; // (Năm, ví dụ: "2010")
    private Integer duration; // (duration_min)
    private String poster; // (URL)
    private String backdrop; // (URL)
    private String trailerUrl; // (Key của Youtube)

    private Boolean isSeries;
    private AgeRating age;
    private MovieStatus status;

    // Dữ liệu quan hệ
    private List<CountryDto> countries;
    private List<GenreDto> genres;
    private PersonDto director;
    private List<PersonDto> actors;

    // (MỚI) Nhận danh sách seasons nếu là TV
    private List<SeasonDto> seasons;
}

// --- DTOs con ---
// (Bạn có thể để chung file hoặc tách riêng)





