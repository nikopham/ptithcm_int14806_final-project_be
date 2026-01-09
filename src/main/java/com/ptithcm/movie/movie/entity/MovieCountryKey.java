package com.ptithcm.movie.movie.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class MovieCountryKey implements Serializable {
  private UUID movieId;
  private Integer countryId;
}
