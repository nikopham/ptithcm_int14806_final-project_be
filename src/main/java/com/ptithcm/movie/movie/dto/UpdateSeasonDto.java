package com.ptithcm.movie.movie.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateSeasonDto {
    private UUID id; // <-- ID (UUID) của Season trong DB
    private String title;
    private List<UpdateEpisodeDto> episodes; // Danh sách các tập đã sửa
}
