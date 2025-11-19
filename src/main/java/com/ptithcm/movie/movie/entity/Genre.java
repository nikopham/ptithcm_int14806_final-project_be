package com.ptithcm.movie.movie.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "genres")
public class Genre {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "tmdb_id", unique = true)
  private Integer tmdbId;

    @Column(unique = true, nullable = false, length = 64)
    private String name;

    /**
     * (MỚI) Map quan hệ ngược để thực hiện JOIN trong query.
     * JsonIgnore để tránh vòng lặp vô tận khi serialize.
     */
    @ManyToMany(mappedBy = "genres")
    @JsonIgnore
    private Set<Movie> movies = new HashSet<>();
}
