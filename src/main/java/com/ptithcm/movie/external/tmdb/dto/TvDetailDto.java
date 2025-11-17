package com.ptithcm.movie.external.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ptithcm.movie.external.tmdb.CountryDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Optional;

@Getter
@Setter
public class TvDetailDto {
    private int id; // -> tmdb_id
    private String name; // -> title
    @JsonProperty("original_name")
    private String originalName;
    private String overview; // -> description
    @JsonProperty("poster_path")
    private String posterPath;
    @JsonProperty("backdrop_path")
    private String backdropPath;
    @JsonProperty("first_air_date")
    private String firstAirDate; // -> release_date
    @JsonProperty("episode_run_time")
    private List<Integer> episodeRunTime; // -> duration_min
    private String status; // -> status

    @JsonProperty("created_by")
    private List<CrewMemberDto> createdBy; // -> (Lưu người tạo)

    private List<GenreDto> genres;
    @JsonProperty("production_countries")
    private List<CountryDto> productionCountries;

    // Danh sách seasons (để điền bảng 'seasons')
    private List<TvSeasonSimpleDto> seasons;

    // --- Dữ liệu gộp (Append-to-response) ---
    private CreditsDtoInternal credits; // (Chứa cast)
    private VideoResultDto videos; // (Chứa trailer)
    @JsonProperty("external_ids")
    private ExternalIdsDto externalIds; // (Chứa IMDB ID)

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
    public int getDuration() {
        return (episodeRunTime != null && !episodeRunTime.isEmpty()) ? episodeRunTime.get(0) : 0;
    }
    public List<CastMemberDto> getCast() {
        return (credits != null) ? credits.cast() : List.of();
    }
    // (Lưu ý: createdBy đã chứa Creator, không cần tìm trong 'credits')

    // ... (Cần Getters/Setters cho tất cả các trường private) ...
}