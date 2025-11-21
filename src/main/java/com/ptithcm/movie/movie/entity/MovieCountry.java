package com.ptithcm.movie.movie.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "movie_countries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovieCountry {

  @EmbeddedId
  private MovieCountryKey id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("movieId")
  @JoinColumn(name = "movie_id")
  private Movie movie;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("countryId")
  @JoinColumn(name = "country_id")
  private Country country;
}