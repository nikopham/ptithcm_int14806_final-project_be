package com.ptithcm.movie.movie.entity;

import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class MovieLikeKey implements java.io.Serializable {
    private UUID userId;
    private UUID movieId;
}
