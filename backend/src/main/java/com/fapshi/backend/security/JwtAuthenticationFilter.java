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
        System.out.println("URL: " + request.getRequestURI());
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                if (jwtUtil.validateToken(token)) {
                    String username = jwtUtil.getUsernameFromToken(token);
                    String role = jwtUtil.getRoleFromToken(token);  // ⬅️ NOUVEAU
                    
                    System.out.println("=== TOKEN INFO ===");
                    System.out.println("Username: " + username);
                    System.out.println("Role from token: " + role);
                    
                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        
                        // Créer l'authority avec le préfixe ROLE_
                        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_" + role)  // ⬅️ AJOUT DU PRÉFIXE
                        );
                        
                        System.out.println("Authorities créées: " + authorities);
                        
                        // Créer un UserDetails simple avec les authorities du token
                        UserDetails userDetails = User.builder()
                            .username(username)
                            .password("")  // Pas besoin du mot de passe pour l'authentification JWT
                            .authorities(authorities)
                            .build();
                        
                        UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                userDetails, 
                                null, 
                                authorities  // ⬅️ UTILISER LES AUTHORITIES DU TOKEN
                            );
                        
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        
                        System.out.println("✅ Authentication configurée avec succès");
                        System.out.println("==================");
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ JWT Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        filterChain.doFilter(request, response);
    }
}