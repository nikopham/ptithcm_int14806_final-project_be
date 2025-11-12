package com.ptithcm.movie.config;

import com.ptithcm.movie.auth.jwt.JwtAuthFilter;
import com.ptithcm.movie.auth.jwt.JwtTokenProvider;
import com.ptithcm.movie.auth.security.CustomOAuth2SuccessHandler;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableRedisHttpSession
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwt;
    private final JwtConfig jwtCfg;
    private final JwtAuthFilter jwtFilter;
    private final RedisConnectionFactory redis;
    private final CorsConfigurationSource corsConfigurationSource;
    private final UserDetailsService uds;
    private final CustomOAuth2SuccessHandler oAuth2SuccessHandler;

//    /* BCrypt */
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }

    /* Session backed by Redis (device limit, revoke) */
    @Bean
    public SpringSessionBackedSessionRegistry sessionRegistry(
            FindByIndexNameSessionRepository<?> repo) {
        return new SpringSessionBackedSessionRegistry<>(repo);
    }

    /* Main DSL */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(c -> c.configurationSource(corsConfigurationSource))
//                .csrf(csrf -> csrf
//                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                /* Google OAuth2 */
                .oauth2Login(o -> o.successHandler(oAuth2SuccessHandler))
                /* add JWT filter */
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                /* headers best-practice */
                .headers(h -> h
                        .xssProtection(Customizer.withDefaults())
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'"))
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true).maxAgeInSeconds(63072000)));
        return http.build();
    }

}

