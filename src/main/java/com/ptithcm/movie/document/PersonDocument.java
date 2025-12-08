package com.ptithcm.movie.document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PersonDocument {
    private String id;
    private String fullName;
    private String job; // ACTOR, DIRECTOR
    private String profilePath;
}