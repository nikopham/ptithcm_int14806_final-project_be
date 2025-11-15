package com.ptithcm.movie.user.entity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.ptithcm.movie.auth.entity.UserOauthAccount;
import com.ptithcm.movie.auth.entity.UserSession;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "users",
       indexes = {
           @Index(name = "idx_users_email_trgm", columnList = "email")
       })
public class User {

    @Id
    @UuidGenerator
    private UUID id;

    /** CITEXT – map như TEXT nhưng case-insensitive ở DB */
    @Column(columnDefinition = "citext", unique = true)
    private String email;

    @Column(columnDefinition = "citext")
    private String username;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "avatar_url")
    private String avatarUrl;

    /* ---------- role ---------- */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    @Column(name = "is_active")
    private Boolean active;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    /* ---------- relations ---------- */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,
               orphanRemoval = true)
    private List<UserOauthAccount> oauthAccounts;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,
               orphanRemoval = true)
    private List<UserSession> sessions;
}
