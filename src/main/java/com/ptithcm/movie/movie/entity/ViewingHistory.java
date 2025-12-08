package com.ptithcm.movie.movie.entity;

import com.ptithcm.movie.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "viewing_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id")
    private Episode episode;

    @Column(name = "watched_seconds")
    private Integer watchedSeconds;

    @Column(name = "total_seconds") // Tổng thời lượng phim (để tính %)
    private Integer totalSeconds;

    @Column(name = "last_watched_at")
    private OffsetDateTime lastWatchedAt;

    @Builder.Default
    private boolean finished = false;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public double getProgressPercent() {
        if (totalSeconds == null || totalSeconds == 0) return 0.0;
        return (double) watchedSeconds / totalSeconds * 100.0;
    }
}