package com.ptithcm.movie.movie.dto.request;

import com.ptithcm.movie.common.constant.AgeRating;
import com.ptithcm.movie.common.constant.MovieStatus;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Data
public class MovieCreateRequest {
    private String title;
    private String originalTitle;
    private String description;

    private Integer releaseYear;
    private Integer durationMin;

    private Boolean isSeries;

    private AgeRating ageRating;
    private MovieStatus status;

    private List<Integer> countryIds;
    private List<Integer> genreIds;

    private List<UUID> directorIds;
    private List<UUID> actorIds;

    private UUID createdBy;

    private MultipartFile posterImage;
    private MultipartFile backdropImage;
}