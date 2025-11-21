package com.ptithcm.movie.recommendation.entity;

import com.ptithcm.movie.movie.entity.Movie;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movie_embeddings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieEmbedding {

    @Id
    @Column(name = "movie_id")
    private UUID movieId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "embedding", columnDefinition = "vector(384)")
    private float[] embedding;
}