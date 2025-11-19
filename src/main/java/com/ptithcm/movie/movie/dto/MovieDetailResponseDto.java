package com.ptithcm.movie.movie.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ptithcm.movie.movie.entity.*;
import lombok.Data;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DTO này "bắt chước" cấu trúc của MovieDetailDto và TvDetailDto
 * nhưng được xây dựng từ Entity 'Movie' trong DB của chúng ta.
 */
@Data
public class MovieDetailResponseDto {

    // (Các trường này khớp với MovieDetailDto/TvDetailDto)
    private int id; // (TMDb ID)
    private String title;
    private String original_name; // (original_title)
    private String overview; // (description)
    private String poster_path; // (poster_url)
    private String backdrop_path; // (backdrop_url)
    private String release_date;
    private int runtime; // (duration_min)
    private String status;

    private List<PublicGenreDto> genres;
    private List<PublicCountryDto> production_countries;
    private PublicPersonDto director;
    private List<PublicPersonDto> cast;

    private String trailer_key;
    private String imdb_id;
    private boolean isSeries;

    // (Trường này chỉ dành cho TV)
    private List<PublicSeasonDto> seasons;
    // (Trường này khớp với 'created_by' của TV)
    private List<PublicPersonDto> created_by;


    // --- DTOs con (Nested DTOs) ---
    // (Chúng ta định nghĩa chúng ở đây cho gọn)

    @Data
    public static class PublicGenreDto {
        private int id; // (TMDb ID)
        private String name;
        public PublicGenreDto(Genre g) {
            this.id = g.getTmdbId();
            this.name = g.getName();
        }
    }

    @Data
    public static class PublicCountryDto {
        @JsonProperty("iso_3166_1") // (Khớp với TMDb key)
        private String iso_code;
        private String name;
        public PublicCountryDto(Country c) {
            this.iso_code = c.getIsoCode();
            this.name = c.getName();
        }
    }

    @Data
    public static class PublicPersonDto {
        private int id; // (TMDb ID)
        private String name;
        @JsonProperty("profile_path")
        private String profile_path;
        public PublicPersonDto(Person p) {
            this.id = p.getTmdbId();
            this.name = p.getFullName();
            this.profile_path = p.getProfilePath();
        }
    }

    @Data
    public static class PublicSeasonDto {
        private int id; // (TMDb ID)
        private String name;
        private int season_number;
        private List<PublicEpisodeDto> episodes; // (Lấy luôn episodes)
        public PublicSeasonDto(Season s) {
            this.id = s.getTmdbId();
            this.name = s.getTitle();
            this.season_number = s.getSeasonNumber();
            // Lấy (và chuyển đổi) episodes
            if (s.getEpisodes() != null) {
                this.episodes = s.getEpisodes().stream()
                        .map(PublicEpisodeDto::new)
                        .collect(Collectors.toList());
            }
        }
    }

    @Data
    public static class PublicEpisodeDto {
        @JsonProperty("id")
        private int tmdb_id;
        private String name;
        private String overview;
        private String air_date;
        private int episode_number;
        private String still_path;
        private int runtime;
        public PublicEpisodeDto(Episode e) {
            this.tmdb_id = e.getTmdbId();
            this.name = e.getTitle();
            this.overview = e.getSynopsis();
            this.air_date = (e.getAirDate() != null) ? e.getAirDate().toString() : null;
            this.episode_number = e.getEpisodeNumber();
            this.still_path = e.getStillPath();
            this.runtime = e.getDurationMin();
        }
    }
}