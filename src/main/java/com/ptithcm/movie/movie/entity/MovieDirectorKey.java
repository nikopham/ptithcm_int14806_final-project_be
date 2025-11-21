package com.ptithcm.movie.movie.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovieDirectorKey implements java.io.Serializable {
  private UUID movieId;
  private UUID personId;
}
