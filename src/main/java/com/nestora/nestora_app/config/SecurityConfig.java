package com.nestora.nestora_app.config;

import com.nestora.nestora_app.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth


                        // PUBLIC API — No token required
                        .requestMatchers(
                                "/auth/**",
                                "/properties/search",
                                "/properties/*/rooms",
                                "/properties/*/reviews",
                                "/properties/*",

                                // NEW — Reels: public browsing (token optional so isLikedByMe
                                // resolves when sent, but works fine without it too)
                                "/reels/feed",
                                "/properties/*/reels",
                                "/reels/*/comments",
                                "/reels/*/view",
                                "/reels/*/share"
                        ).permitAll()

                        // WebSocket public
                        .requestMatchers("/ws/**").permitAll()

                        // =============================================
                        // ADMIN ONLY
                        // =============================================
                        .requestMatchers("/admin/**").hasAuthority("ADMIN")
                        // NOTE: this already covers /admin/withdrawals/** (Refer & Earn) too —
                        // no separate line needed since it's a sub-path of /admin/**

                        // =============================================
                        // OWNER — Verified owner
                        // =============================================
                        .requestMatchers(
                                "/owner/properties/**",
                                "/owner/booking-requests/**",
                                "/owner/dashboard",
                                "/owner/visits/**",
                                "/owner/reels/**" // NEW — Reels: upload/list/delete own reels
                        ).hasAnyAuthority("OWNER", "ADMIN")

                        // Owner apply — koi bhi logged in user
                        .requestMatchers(
                                "/owner/apply",
                                "/owner/my-profile",
                                "/owner/verification-status",
                                "/owner/subscription/**",
                                "/owner/property-access/**"
                        ).hasAnyAuthority("STUDENT", "OWNER", "ADMIN")

                        // =============================================
                        // ALL AUTHENTICATED USERS
                        // =============================================
                        // NOTE: /reels/{id}/like, /reels/{id}/comments (POST), /user/referral/**,
                        // and /user/wallet/** all fall through to here automatically — any
                        // logged-in user (STUDENT/OWNER/ADMIN) can hit them, which is correct,
                        // no extra line needed for them.
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}