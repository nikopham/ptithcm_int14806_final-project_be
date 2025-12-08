package com.ptithcm.movie.movie.dto.response;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class PersonDetailResponse {
    private UUID id;
    private String fullName;
    private String biography;
    private LocalDate birthDate;
    private String placeOfBirth;
    private String profilePath;
    private String job;

    private Page<MovieSearchResponse> movies;
}