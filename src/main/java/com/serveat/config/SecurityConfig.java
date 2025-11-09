package com.serveat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // desactiva CSRF para evitar bloqueos
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/carta", "/images/**", "/VAADIN/**").permitAll() // deja pasar la carta
                        .anyRequest().permitAll() // permite todo temporalmente
                );
        return http.build();
    }
}