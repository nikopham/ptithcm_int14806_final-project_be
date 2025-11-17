package com.ptithcm.movie.movie.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.ptithcm.movie.common.constant.AgeRating;
import com.ptithcm.movie.common.constant.VideoQuality;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.ptithcm.movie.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "movies",
       indexes = { @Index(name = "idx_movies_title_trgm", columnList = "title") }
)
public class Movie {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(length = 256, nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "duration_min")
    private Integer durationMin;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_rating", length = 5)
    private AgeRating ageRating;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(name = "is_series")
    private Boolean series;

    @Column
    private Long viewCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private ContentStatus status;

    @Column(unique = true)
    private String slug;

    @Column(unique = true)
    private Integer tmdbId;

    @Column(name = "imdb_id", unique = true, length = 16) // <-- Sửa: VARCHAR(16)
    private String imdbId;

    @Column(name = "imdb_score", precision = 3, scale = 1)
    private BigDecimal imdbScore;

    @Column
    private String originalTitle;

    @Column
    private String backdropUrl;

    @Column
    private String trailerUrl;

    @Column
    private String videoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality", length = 16)
    private VideoQuality quality;

    @ManyToOne(fetch = FetchType.LAZY)          // created_by
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
            name = "movie_countries",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "country_id")
    )
    private Set<Country> countries = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
            name = "movie_genres",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
            name = "movie_actors",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "person_id")
    )
    private Set<Person> actors = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
            name = "movie_directors",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "person_id")
    )
    private Set<Person> directors = new HashSet<>();

    /* -------- relations -------- */
    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL,
               orphanRemoval = true)
    private List<Season> seasons;

    /* -------- enum -------- */
    public enum ContentStatus { DRAFT, HIDDEN, PUBLISHED }
}