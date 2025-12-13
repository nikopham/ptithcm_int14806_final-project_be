package com.ptithcm.movie.document;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PersonDocument {
    private String id;
    private String fullName;
    private List<String> job; // ACTOR, DIRECTOR
    private String profilePath;
}