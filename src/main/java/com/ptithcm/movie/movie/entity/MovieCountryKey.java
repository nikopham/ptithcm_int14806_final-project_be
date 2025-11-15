package com.ptithcm.movie.movie.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Embeddable;

@Embeddable
public class MovieCountryKey implements Serializable {
  private UUID movieId;
  private Integer countryId;
}
