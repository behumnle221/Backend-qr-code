package com.fapshi.backend.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Utilitaire pour hacher et vérifier les mots de passe.
 * Utilise BCrypt avec salage automatique.
 * Respecte SOLID : Single Responsibility (seulement gestion password).
 */
@Component
public class PasswordEncoderUtil {

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public PasswordEncoderUtil(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Hache un mot de passe avec salt automatique
     */
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Vérifie si un mot de passe en clair correspond au hash stocké
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}