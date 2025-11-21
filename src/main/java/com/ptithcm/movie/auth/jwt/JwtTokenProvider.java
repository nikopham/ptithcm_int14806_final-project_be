package com.ptithcm.movie.auth.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.ptithcm.movie.config.JwtConfig;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final JwtConfig cfg;
    private final Algorithm alg;

    public JwtTokenProvider(JwtConfig cfg) {
        this.cfg = cfg;
        this.alg = Algorithm.HMAC256(cfg.getSecret());
    }

    public String generateAccessToken(UserDetails user) {
        return JWT.create()
                .withSubject(user.getUsername())
                .withClaim("roles", user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).toList())
                .withExpiresAt(Instant.now().plus(cfg.getAccessTtlMin(), ChronoUnit.MINUTES))
                .sign(alg);
    }

    public String generateRefreshToken(UUID userId, UUID jti) {
        return JWT.create()
                .withSubject(userId.toString())
                .withJWTId(jti.toString())
                .withExpiresAt(Instant.now().plus(cfg.getRefreshTtlDay(), ChronoUnit.DAYS))
                .sign(alg);
    }

    public DecodedJWT verify(String token) {
        return JWT.require(alg).build().verify(token); }
}
