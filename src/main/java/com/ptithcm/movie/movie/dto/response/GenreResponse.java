package com.ptithcm.movie.movie.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GenreResponse {
    private Integer id;
    private String name;
    private Long movieCount;

}