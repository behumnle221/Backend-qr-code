package com.fapshi.backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)  // Désactive CORS complètement pour test
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/api/webhook/**").permitAll()  // 🔶 Webhooks d'Aangaraa (pas d'authentification)
    .requestMatchers("/error").permitAll()
    .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
    .requestMatchers("/v3/api-docs/**").permitAll()
    
    // QR generation : vendeurs seulement
    .requestMatchers("/api/qr/generate").hasRole("VENDEUR")
    
    // Solde vendeur : vendeurs seulement
    .requestMatchers("/api/vendeur/**").hasRole("VENDEUR")
    
    // Paiement initiation : clients (ou clients + vendeurs)
    .requestMatchers("/api/payments/initiate").hasAnyRole("CLIENT", "VENDEUR")
    
    // Autres endpoints paiement : authentifiés
    .requestMatchers("/api/payments/**").authenticated()
    
    // Tout le reste nécessite authentification
    .anyRequest().authenticated()
)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}