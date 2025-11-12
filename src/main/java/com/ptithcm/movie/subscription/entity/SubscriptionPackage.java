package com.ptithcm.movie.subscription.entity;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "subscription_packages")
public class SubscriptionPackage {

    @Id @UuidGenerator
    private UUID id;

    @Column(length = 64, nullable = false, unique = true)
    private String name;

    @Column(name = "monthly_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal monthlyPrice;

    @Column(name = "max_quality", length = 16, nullable = false)
    private String maxQuality;          // 1080p, 4K …

    @Column(name = "device_limit", nullable = false)
    private Integer deviceLimit;

    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    /* -------- relations -------- */
    @OneToMany(mappedBy = "pack")
    private List<UserSubscription> userSubscriptions;
}