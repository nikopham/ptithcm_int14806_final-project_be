package com.ptithcm.movie.movie.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.ptithcm.movie.common.constant.VideoQuality;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.ptithcm.movie.user.entity.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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

    @Column(name = "age_rating", length = 8)
    private String ageRating;

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

    @Column(unique = true)
    private Integer imdbId;

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

    /* -------- relations -------- */
    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL,
               orphanRemoval = true)
    private List<Season> seasons;

    /* -------- enum -------- */
    public enum ContentStatus { DRAFT, HIDDEN, PUBLISHED }
}