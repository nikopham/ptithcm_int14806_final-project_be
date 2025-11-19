package com.ptithcm.movie.movie.entity;


import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

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
@Table(name = "episodes")
public class Episode {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(name = "episode_number", nullable = false)
    private Integer episodeNumber;

    @Column(length = 256)
    private String title;

    @Column(name = "duration_min")
    private Integer durationMin;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "video_url", columnDefinition = "TEXT")
    private String videoUrl;

    @Column(name = "still_path", columnDefinition = "TEXT")
    private String stillPath;

    @Column(name = "air_date")
    private LocalDate airDate;

    @Column(name = "tmdb_id")
    private Integer tmdbId;
}