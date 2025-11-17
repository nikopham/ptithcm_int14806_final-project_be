package com.ptithcm.movie.external.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ptithcm.movie.external.tmdb.CountryDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
// Dùng Lombok @Data hoặc tự viết Getter/Setter
public class MovieDetailDto {
    private int id; // -> tmdb_id
    private String title;
    @JsonProperty("original_title")
    private String originalTitle;
    private String overview; // -> description
    @JsonProperty("release_date")
    private String releaseDate;
    private int runtime; // -> duration_min
    @JsonProperty("poster_path")
    private String posterPath;
    @JsonProperty("backdrop_path")
    private String backdropPath;
    private List<GenreDto> genres;
    @JsonProperty("production_countries")
    private List<CountryDto> productionCountries;

    // --- Dữ liệu gộp (Append-to-response) ---
    private CreditsDtoInternal credits;
    private VideoResultDto videos;
    @JsonProperty("external_ids")
    private ExternalIdsDto externalIds;

    // --- Helpers ---
    public String getTrailerKey() {
        if (videos == null || videos.results() == null) return null;
        return videos.results().stream()
                .filter(v -> "Trailer".equalsIgnoreCase(v.type()) && "YouTube".equalsIgnoreCase(v.site()))
                .findFirst().map(VideoDto::key).orElse(null);
    }
    public String getImdbId() {
        return (externalIds != null) ? externalIds.imdbId() : null;
    }
    public CrewMemberDto getDirector() {
        if (credits == null || credits.crew() == null) return null;
        return credits.crew().stream()
                .filter(c -> "Director".equalsIgnoreCase(c.job()))
                .findFirst().orElse(null);
    }
    public List<CastMemberDto> getCast() {
        return (credits != null) ? credits.cast() : List.of();
    }

    // ... (Cần Getters/Setters cho tất cả các trường private) ...
}