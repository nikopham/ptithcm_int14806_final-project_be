package com.ptithcm.movie.movie.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonResponse {
    private java.util.UUID id;
    private String fullName;
    private List<String> job;
    private String profilePath;
    private Long movieCount;
}
