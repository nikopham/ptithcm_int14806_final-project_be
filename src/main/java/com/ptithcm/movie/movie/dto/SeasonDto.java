package com.ptithcm.movie.movie.dto;


import lombok.Data;

@Data
public class SeasonDto {
    // Dữ liệu này lấy từ 'tvDetail.seasons'
    private Integer id; // (TMDb ID của Season)
    private String name;
    private Integer season_number;
}