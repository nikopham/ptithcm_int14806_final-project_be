package com.ptithcm.movie.movie.dto.request;
import lombok.Data;
@Data
public class ReviewSearchRequest {
    private String query;
    private Integer rating;
}