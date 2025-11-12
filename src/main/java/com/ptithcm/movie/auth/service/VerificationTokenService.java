package com.ptithcm.movie.auth.service;

import com.ptithcm.movie.auth.entity.VerificationToken;
import com.ptithcm.movie.auth.repository.VerificationTokenRepository;
import com.ptithcm.movie.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationTokenService {
    private final VerificationTokenRepository repo;

    public VerificationToken create(User user) {
        String token = UUID.randomUUID().toString();
        VerificationToken vt = VerificationToken.builder()
                .token(token)
                .user(user)
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .build();
        return repo.save(vt);
    }

    public User confirm(String token) {
        VerificationToken vt = repo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        if (vt.getExpiresAt().isBefore(OffsetDateTime.now()))
            throw new IllegalStateException("Token expired");

        User user = vt.getUser();
        user.setEmailVerified(true);
        return user; // caller sẽ save(user)
    }
}