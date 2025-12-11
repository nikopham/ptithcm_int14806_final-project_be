package com.ptithcm.movie.movie.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardOverviewResponse {
    private long totalUsers;
    private long totalMovies;
    private long totalViews;
    private long totalComments;
}