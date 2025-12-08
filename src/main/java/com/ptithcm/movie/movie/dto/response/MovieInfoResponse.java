package com.ptithcm.movie.movie.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MovieInfoResponse {
    private UUID id;
    private String title;
    private String originalTitle;
    private String description;

    @JsonProperty("release")
    private String releaseYear;

    private Integer duration;

    @JsonProperty("age")
    private String ageRating;

    private String status;

    private List<CountryDto> countries;
    private List<GenreDto> genres;

    private PersonDto director;
    private List<PersonDto> actors;

    @JsonProperty("poster")
    private String posterUrl;

    @JsonProperty("backdrop")
    private String backdropUrl;

    private Double averageRating;
    private Long viewCount;
    private String trailerUrl;
    private String slug;
    private boolean isSeries;
    private String videoUrl;
    private List<SeasonDto> seasons;

    @Data
    @Builder
    public static class CountryDto {
        private Integer id;
        private String isoCode;
        private String name;
    }

    @Data
    @Builder
    public static class GenreDto {
        private Integer id;
        private String name;
        private Long movieCount;
    }

    @Data
    @Builder
    public static class PersonDto {
        private UUID id;
        private String name;
        private String avatar;
    }

    @Data
    @Builder
    public static class SeasonDto {
        private UUID id;
        private Integer seasonNumber;
        private String title;
        private List<EpisodeDto> episodes;
    }

    @Data
    @Builder
    public static class EpisodeDto {
        private UUID id;
        private Integer episodeNumber;
        private String title;
        private Integer duration;
        private String synopsis;
        private String stillPath;
        private LocalDate airDate;
    }
}