package com.ptithcm.movie.auth.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "auth_providers")
public class AuthProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  
    private Short id;

    @Column(name = "provider_key", length = 32, unique = true, nullable = false)
    private String providerKey;   // LOCAL, GOOGLE …

    @Column(name = "display_name", length = 64)
    private String displayName;

    @OneToMany(mappedBy = "provider")
    private List<UserOauthAccount> accounts;
}
