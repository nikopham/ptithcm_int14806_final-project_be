package com.ptithcm.movie.movie.entity;

import com.ptithcm.movie.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.Set;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(name = "original_title")
    private String originalTitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(unique = true)
    private String slug;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "duration_min")
    private Integer durationMin;

    @Column(name = "age_rating", length = 8)
    private String ageRating;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(name = "backdrop_url")
    private String backdropUrl;

    @Column(name = "trailer_url")
    private String trailerUrl;

    @Column(name = "video_url")
    private String videoUrl;

    @Builder.Default
    private String quality = "FHD";

    @Column(name = "is_series")
    private boolean isSeries;

    @Builder.Default
    private String status = "PUBLISHED";

    @Column(name = "view_count")
    private Long viewCount;

    // Mapping External IDs
    @Column(name = "movielens_id", unique = true)
    private Integer movielensId;

    @Column(name = "imdb_id")
    private String imdbId;

    @Column(name = "imdb_score")
    private BigDecimal imdbScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // --- RELATIONS ---

    @ManyToMany
    @JoinTable(
            name = "movie_genres",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "movie_countries",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "country_id")
    )
    private Set<Country> countries = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "movie_actors",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "person_id")
    )
    private Set<Person> actors = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "movie_directors",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "person_id")
    )
    private Set<Person> directors = new HashSet<>();
}