package com.ptithcm.movie.recommendation.entity;

import com.ptithcm.movie.movie.entity.Movie;
import com.ptithcm.movie.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="movie_embeddings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovieEmbedding {

    @Id
    @Column(name="movie_id")
    private UUID movieId;

    @OneToOne(fetch=FetchType.LAZY) @MapsId
    @JoinColumn(name="movie_id")
    private Movie movie;

    @JdbcTypeCode(SqlTypes.VECTOR_FLOAT32)
    @Column(columnDefinition = "vector(256)")
    private float[] embedding;
}