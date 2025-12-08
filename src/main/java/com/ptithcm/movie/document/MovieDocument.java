package com.ptithcm.movie.document;

import lombok.Builder;
import lombok.Data;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MovieDocument {
    private String id;
    private String title;
    private String originalTitle;
    private String description;
    private String slug;
    private String poster;
    private Double rating;
    private Integer releaseYear;
    private Boolean isSeries;
    private String status;
    private List<String> genres; // ["Action", "Drama"]
}