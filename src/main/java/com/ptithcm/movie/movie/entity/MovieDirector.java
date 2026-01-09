package com.ptithcm.movie.movie.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "movie_directors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovieDirector {

  @EmbeddedId
  private MovieDirectorKey id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("movieId")
  @JoinColumn(name = "movie_id")
  private Movie movie;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("personId")
  @JoinColumn(name = "person_id")
  private Person people;
}
