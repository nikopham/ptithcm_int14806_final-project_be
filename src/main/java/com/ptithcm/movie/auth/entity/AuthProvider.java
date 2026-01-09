package com.ptithcm.movie.auth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "auth_providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Column(name = "provider_key", nullable = false, unique = true, length = 32)
    private String providerKey; // google, facebook

    @Column(name = "display_name", length = 64)
    private String displayName;
}