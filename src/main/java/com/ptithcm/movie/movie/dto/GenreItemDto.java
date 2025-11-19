package com.ptithcm.movie.movie.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor // Cần cái này cho JPQL 'new GenreItemDto(...)'
@NoArgsConstructor
public class GenreItemDto {
    private Integer id;
    private String name;
    private Integer tmdbId;
    private Long movieCount; // Số lượng phim thuộc genre này
}