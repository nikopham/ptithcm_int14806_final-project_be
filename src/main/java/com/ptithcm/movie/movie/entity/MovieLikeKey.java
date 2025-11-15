package com.ptithcm.movie.movie.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class MovieLikeKey implements Serializable {
    private UUID userId;
    private UUID movieId;
}
