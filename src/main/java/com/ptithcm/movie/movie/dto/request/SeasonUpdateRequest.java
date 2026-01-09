package com.ptithcm.movie.movie.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class SeasonUpdateRequest {
    private Integer seasonNumber;
    private String title;
}
