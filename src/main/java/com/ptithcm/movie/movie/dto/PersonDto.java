package com.ptithcm.movie.movie.dto;

import lombok.Data;

@Data
public class PersonDto {
    private Integer id; // (Đây là tmdb_id)
    private String name;
    private String img; // (profile_path)
}
