package com.ptithcm.movie.auth.entity;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.ptithcm.movie.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "user_oauth_accounts",
       uniqueConstraints = @UniqueConstraint(
         name = "uk_provider_user",
         columnNames = {"provider_id", "provider_user_id"}))
public class UserOauthAccount {

    @Id
    @UuidGenerator
    private UUID id;

    /* ---------- FK ---------- */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private AuthProvider provider;

    @Column(name = "provider_user_id", length = 256, nullable = false)
    private String providerUserId;

    @Column(columnDefinition = "citext")
    private String email;

    private Boolean emailVerified;
}
