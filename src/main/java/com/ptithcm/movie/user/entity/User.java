package com.ptithcm.movie.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(columnDefinition = "citext", unique = true)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private String email;

    @Column(columnDefinition = "citext")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private String username;

    @Column(name = "password_hash")
    private String passwordHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(name = "email_verified")
    private boolean emailVerified;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "is_imported")
    private boolean isImported;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}