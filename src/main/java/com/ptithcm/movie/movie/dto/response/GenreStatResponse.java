package com.ptithcm.movie.movie.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GenreStatResponse {
    private String genreName;
    private Long movieCount;
}
