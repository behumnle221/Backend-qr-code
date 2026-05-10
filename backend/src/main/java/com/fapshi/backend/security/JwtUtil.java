package com.fapshi.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    // ✅ Clé fixe chargée depuis application.properties / variable d'environnement
    // La clé ne change PLUS à chaque redémarrage du serveur
    private final SecretKey secretKey;
    private final long expirationTime;

    public JwtUtil(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration:86400000}") long jwtExpiration) {
        // Dériver une clé HMAC-SHA512 depuis la chaîne de caractères
        byte[] keyBytes = Base64.getEncoder().encode(
                jwtSecret.getBytes(StandardCharsets.UTF_8)
        );
        // Tronquer ou padder à 64 octets (512 bits) pour HS512
        byte[] keyFixed = new byte[64];
        System.arraycopy(keyBytes, 0, keyFixed, 0, Math.min(keyBytes.length, 64));
        this.secretKey = new SecretKeySpec(keyFixed, "HmacSHA512");
        this.expirationTime = jwtExpiration;
        log.info("✅ JwtUtil initialisé avec une clé secrète FIXE (stable entre redémarrages)");
    }

    public String generateToken(String username, Long userId, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(secretKey)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    public Long getUserIdFromToken(String token) {
        return getClaims(token).get("userId", Long.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token expiré");
            return false;
        } catch (Exception e) {
            log.warn("Token invalide: {}", e.getMessage());
            return false;
        }
    }
}