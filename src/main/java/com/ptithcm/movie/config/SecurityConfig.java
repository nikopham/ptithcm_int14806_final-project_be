package com.ptithcm.movie.config;

import com.ptithcm.movie.auth.jwt.JwtAuthFilter;
import com.ptithcm.movie.auth.jwt.JwtTokenProvider;
import com.ptithcm.movie.auth.security.CustomOAuth2SuccessHandler;
import com.ptithcm.movie.common.constant.GlobalConstant;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@EnableRedisHttpSession
@RequiredArgsConstructor
@ComponentScan("com.ptithcm.movie")
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
                        // Public
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/v1/genres/featured",
                                "/api/v1/movies/top-10",
                                "/api/v1/genres/get-all",
                                "/api/v1/countries/get-all",
                                "/api/v1/movies/search",
                                "/api/v1/movies/detail/**",
                                "/api/v1/movies/*/comments",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Viewer + các admin: xem thông tin user, list phim đã like
                        .requestMatchers(
                                "/api/users/me",
                                "/api/v1/movies/search-liked",
                                "/api/v1/comments/**",
                                "/api/v1/movies/like/**"
                        ).hasAnyAuthority(
                                GlobalConstant.ROLE_VIEWER,
                                GlobalConstant.ROLE_SUPER_ADMIN,
                                GlobalConstant.ROLE_MOVIE_ADMIN,
                                GlobalConstant.ROLE_COMMENT_ADMIN
                        )

                        .requestMatchers(
                                "/api/v1/reviews/add",
                                "/api/v1/reviews/update/**"

                        ).hasAnyAuthority(GlobalConstant.ROLE_VIEWER)

                        .requestMatchers(
                                "/api/v1/movies/**",
                                "/api/v1/upload/**",
                                "/api/v1/seasons/**",
                                "/api/v1/genres/**"
                        ).hasAnyAuthority(GlobalConstant.ROLE_MOVIE_ADMIN, GlobalConstant.ROLE_SUPER_ADMIN)

                        .requestMatchers("/api/v1/**")
                            .hasAuthority(GlobalConstant.ROLE_SUPER_ADMIN)

                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("""
                        {"success":false,"code":401,"message":"Unauthorized – missing or invalid token"}
                        """);
                        }))
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

