package com.ptithcm.movie.movie.entity;

import com.ptithcm.movie.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "movie_likes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieLike {

    @EmbeddedId
    private MovieLikeId id;

    @MapsId("userId")
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("movieId")
    @ManyToOne
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}