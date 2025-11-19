package com.cinematch.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfigPlaceholder {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Health check - always public
                        .requestMatchers("/api/health").permitAll()

                        // Auth endpoints προσωρινά public (US5/US6)
                        .requestMatchers("/auth/**").permitAll()

                        // Όλα τα υπόλοιπα προσωρινά public (placeholder mode)
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}