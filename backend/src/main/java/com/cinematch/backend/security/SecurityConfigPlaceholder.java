package com.cinematch.backend.security;

import com.cinematch.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfigPlaceholder {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Health check - always public
                        .requestMatchers("/api/health").permitAll()

                        // Auth endpoints public
                        .requestMatchers("/auth/**").permitAll()

                        // 🔥 ADMIN-ONLY endpoints
                        .requestMatchers("/admin/**").hasRole("ADMIN")


                        // QUIZ: μόνο authenticated users (USER + ADMIN)
                        .requestMatchers("/quiz/**").authenticated()

                        // 🔥 USER-ONLY endpoints
                        .requestMatchers("/user/**").hasRole("USER")

                        // 🔥 USER + ADMIN μπορούν να δουν ταινίες, trending, search
                        .requestMatchers("/movies/**").permitAll()

                        // Όλα τα υπόλοιπα προσωρινά public (placeholder mode)
                        .anyRequest().permitAll()
                )

                // 🔥 Εδώ προσθέτουμε το JWT φίλτρο πριν το default filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
