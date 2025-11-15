package com.ptithcm.movie.movie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "movie_actors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovieActor {

  @EmbeddedId
  private MovieActorKey id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("movieId")
  @JoinColumn(name = "movie_id")
  private Movie movie;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("personId")
  @JoinColumn(name = "person_id")
  private Person person;
}