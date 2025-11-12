package com.ptithcm.movie.movie.entity;

import java.util.UUID;

import jakarta.persistence.Embeddable;

@Embeddable
public class MovieCountryKey implements java.io.Serializable {
  private UUID movieId;
  private Integer countryId;
}
