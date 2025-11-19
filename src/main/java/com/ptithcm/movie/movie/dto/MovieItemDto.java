package com.ptithcm.movie.movie.dto;

import com.ptithcm.movie.common.constant.AgeRating;
import com.ptithcm.movie.common.constant.MovieStatus;
import com.ptithcm.movie.movie.entity.Movie;
import lombok.Data;

@Data
public class MovieItemDto {

    private String id; // (UUID)
    private String title;
    private String poster; // (poster_url)
    private String release; // (release_date)
    private Integer duration; // (duration_min)
    private AgeRating age; // (age_rating)
    private MovieStatus status;
    private Long view; // (view_count)
    private Boolean series; // (is_series)

    /**
     * Constructor để "biến đổi" (transform) từ Entity sang DTO
     */
    public MovieItemDto(Movie movie) {
        this.id = movie.getId().toString();
        this.title = movie.getTitle();
        this.poster = movie.getPosterUrl();
        this.release = (movie.getReleaseDate() != null)
                ? movie.getReleaseDate().toString()
                : "—";
        this.duration = movie.getDurationMin();
        this.age = movie.getAgeRating();
        this.status = movie.getStatus();
        this.view = movie.getViewCount();
        this.series = movie.getIsSeries();
    }
}
