package com.nestora.nestora_app.config;

import com.nestora.nestora_app.filter.JwtAuthFilter;
import com.nestora.nestora_app.filter.RateLimitFilter;
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

                        // =============================================
                        // PUBLIC — No token required
                        // =============================================
                        .requestMatchers(
                                "/auth/**",
                                "/properties/search",
                                "/properties/*/rooms",
                                "/properties/*/reviews",
                                "/properties/*"
                        ).permitAll()

                        // WebSocket public
                        .requestMatchers("/ws/**").permitAll()

                        // =============================================
                        // ADMIN ONLY
                        // =============================================
                        .requestMatchers("/admin/**").hasAuthority("ADMIN")

                        // =============================================
                        // OWNER — Verified owner
                        // =============================================
                        .requestMatchers(
                                "/owner/properties/**",
                                "/owner/booking-requests/**",
                                "/owner/dashboard",
                                "/owner/visits/**"
                        ).hasAnyAuthority("OWNER", "ADMIN")

                        // Owner apply — koi bhi logged in user
                        .requestMatchers(
                                "/owner/apply",
                                "/owner/my-profile",
                                "/owner/verification-status",
                                "/owner/subscription/**"
                        ).hasAnyAuthority("STUDENT", "OWNER", "ADMIN")

                        // =============================================
                        // ALL AUTHENTICATED USERS
                        // =============================================
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