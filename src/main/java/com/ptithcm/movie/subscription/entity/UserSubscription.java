package com.ptithcm.movie.subscription.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.ptithcm.movie.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "user_subscriptions")
public class UserSubscription {

    @Id @UuidGenerator
    private UUID id;

    /* -------- FK -------- */
    @ManyToOne(fetch = FetchType.LAZY)          // user_id
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)          // package_id
    @JoinColumn(name = "package_id")
    private SubscriptionPackage pack;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private OffsetDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private SubscriptionStatus status;

    /* ---------- enum ---------- */
    public enum SubscriptionStatus { ACTIVE, CANCELED, EXPIRED }
}
