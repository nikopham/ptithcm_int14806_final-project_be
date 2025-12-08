package com.ptithcm.movie.movie.entity;


import com.ptithcm.movie.common.constant.VideoUploadStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "episodes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"season_id", "episode_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Episode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id")
    private Season season;

    @Column(name = "episode_number", nullable = false)
    private Integer episodeNumber;

    private String title;

    @Column(name = "duration_min")
    private Integer durationMin;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "still_path")
    private String stillPath;

    @Column(name = "air_date")
    private LocalDate airDate;

    @Column(name = "video_status")
    @Enumerated(EnumType.STRING)
    private VideoUploadStatus videoStatus;
}