package com.fapshi.backend.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") || 
               path.startsWith("/swagger-ui") || 
               path.startsWith("/v3/api-docs");
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        System.out.println("=== JWT Filter EXECUTING ===");
        System.out.println("URL demandée : " + request.getRequestURI());
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            System.out.println("Token reçu (début) : " + token.substring(0, Math.min(30, token.length())) + "...");
            
            try {
                if (jwtUtil.validateToken(token)) {
                    String username = jwtUtil.getUsernameFromToken(token);
                    String role = jwtUtil.getRoleFromToken(token);
                    
                    System.out.println("=== INFOS DU TOKEN ===");
                    System.out.println("Username extrait : " + username);
                    System.out.println("Rôle brut extrait : '" + role + "'");
                    
                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        
                        // Créer l'authority avec le préfixe ROLE_
                        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_" + role)
                        );
                        
                        System.out.println("Authorities créées : " + authorities);
                        
                        // Créer un UserDetails simple
                        UserDetails userDetails = User.builder()
                            .username(username)
                            .password("")  
                            .authorities(authorities)
                            .build();
                        
                        UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                userDetails, 
                                null, 
                                authorities
                            );
                        
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        
                        System.out.println("✅ Authentification configurée avec succès");
                        System.out.println("Utilisateur authentifié : " + authToken.getName());
                        System.out.println("Authorities finales reconnues par Spring : " + authToken.getAuthorities());
                        System.out.println("==================");
                    }
                } else {
                    System.out.println("Token invalide ou expiré");
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur JWT : " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Aucun header Authorization ou pas de Bearer");
        }
        
        filterChain.doFilter(request, response);
    }
}