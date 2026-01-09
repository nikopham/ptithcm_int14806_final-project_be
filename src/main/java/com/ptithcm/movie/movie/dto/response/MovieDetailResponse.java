package com.ptithcm.movie.movie.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MovieDetailResponse {
    // --- 1. Basic Info ---
    private UUID id;
    private String title;
    private String originalTitle;
    private String description;
    private String slug;

    private String posterUrl;
    private String backdropUrl;
    private String trailerUrl;

    // --- 2. Technical & Status ---
    private Integer releaseYear;
    private LocalDate releaseDate;
    private Integer durationMin;
    private String ageRating;
    private String quality;     // FHD, 4K
    private String status;      // PUBLISHED
    private boolean isSeries;
    private boolean isLiked;
    private Long currentSecond;

    // --- 3. Statistics ---
    private Double averageRating;
    private Integer reviewCount;
    private Long viewCount;
    private String videoUrl;


    // --- 4. Relationships ---
    private List<GenreResponse> genres;     // {id, name}
    private List<CountryResponse> countries; // {id, name, isoCode}

    // People (Kèm movieCount)
    private List<PersonResponse> directors;
    private List<PersonResponse> actors;

    // Series Content (Null nếu là phim lẻ)
    private List<SeasonDto> seasons;

    @Data @Builder
    public static class SeasonDto {
        private UUID id;
        private Integer seasonNumber;
        private String title;
        private List<EpisodeDto> episodes;
    }

    @Data @Builder
    public static class EpisodeDto {
        private UUID id;
        private Integer episodeNumber;
        private String title;
        private Integer durationMin;
        private String videoUrl;
        private String synopsis;
        private String stillPath;
        private LocalDate airDate;
        private Long currentSecond;
    }
}