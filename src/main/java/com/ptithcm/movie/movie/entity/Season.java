package com.ptithcm.movie.movie.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "seasons")
public class Season {

    @Id
    @UuidGenerator
    private UUID id;

    /* -------- FK -------- */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;

    @Column(length = 128)
    private String title;

    /* -------- relations -------- */
    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL,
               orphanRemoval = true)
    private List<Episode> episodes;
}
