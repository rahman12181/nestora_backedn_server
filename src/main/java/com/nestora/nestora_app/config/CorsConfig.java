package com.nestora.nestora_app.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Sirf yeh domains allow karo
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",        // Next.js dev
                "http://localhost:5173",        // Vite dev
                "https://nestora.in",           // Production website
                "https://www.nestora.in",       // Production www
                "https://admin.nestora.in"      // Admin panel
        ));

        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        config.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Refresh-Token",
                "X-Requested-With"
        ));

        config.setExposedHeaders(Arrays.asList(
                "X-Rate-Limit-Remaining"
        ));

        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}