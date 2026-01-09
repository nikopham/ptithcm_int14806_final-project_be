package com.ptithcm.movie.auth.repository;


import com.ptithcm.movie.auth.entity.AuthProvider;
import com.ptithcm.movie.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthProviderRepository
        extends JpaRepository<AuthProvider, UUID> {
    Optional<AuthProvider> findByProviderKey(String providerKey);
}

