package com.ptithcm.movie.recommendation.entity;

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
@Table(name="user_embeddings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserEmbedding {

    @Id
    @Column(name="user_id")
    private UUID userId;

    @OneToOne(fetch=FetchType.LAZY) @MapsId
    @JoinColumn(name="user_id")
    private User user;

    @JdbcTypeCode(SqlTypes.VECTOR_FLOAT32)
    @Column(columnDefinition = "vector(256)")
    private float[] embedding;

    @UpdateTimestamp
    @Column(name="updated_at")
    private OffsetDateTime updatedAt;
}