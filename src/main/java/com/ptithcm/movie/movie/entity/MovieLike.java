package com.ptithcm.movie.movie.entity;

import com.ptithcm.movie.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "movie_likes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovieLike {
    @EmbeddedId
    private MovieLikeKey id;
    @ManyToOne(fetch= FetchType.LAZY) @MapsId("userId")
    @JoinColumn(name="user_id")   private User user;
    @ManyToOne(fetch=FetchType.LAZY) @MapsId("movieId")
    @JoinColumn(name="movie_id")  private Movie movie;
    @CreationTimestamp
    @Column(name="created_at", updatable=false)
    private OffsetDateTime createdAt;
}