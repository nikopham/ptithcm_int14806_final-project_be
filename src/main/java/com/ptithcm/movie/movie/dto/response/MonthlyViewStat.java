package com.ptithcm.movie.movie.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MonthlyViewStat {
    private Integer month;
    private Long viewCount;
}
