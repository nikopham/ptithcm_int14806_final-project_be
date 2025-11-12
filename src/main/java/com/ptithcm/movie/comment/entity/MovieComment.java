package com.ptithcm.movie.comment.entity;


import com.ptithcm.movie.movie.entity.Movie;
import com.ptithcm.movie.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name="movie_comments",
        indexes=@Index(name="idx_comment_sentiment", columnList="sentiment_score"))
public class MovieComment {

    @Id @UuidGenerator private UUID id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id")
    private User user;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="movie_id")
    private Movie movie;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="parent_id")
    private MovieComment parent;

    @Column(columnDefinition="text", nullable=false) private String body;

    @Column(name="sentiment_score", precision=4, scale=3)
    private BigDecimal sentimentScore;

    private Boolean isHidden = false;

    @CreationTimestamp
    @Column(name="created_at", updatable=false)
    private OffsetDateTime createdAt;
}