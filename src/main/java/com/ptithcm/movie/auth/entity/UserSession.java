package com.ptithcm.movie.auth.entity;

import com.ptithcm.movie.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "jwt_id", unique = true)
    private UUID jwtId;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "ip_address", columnDefinition = "inet")
    @JdbcTypeCode(SqlTypes.OTHER)
    private String ipAddress; // PostgreSQL INET map to String in Java

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Builder.Default
    private boolean revoked = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}