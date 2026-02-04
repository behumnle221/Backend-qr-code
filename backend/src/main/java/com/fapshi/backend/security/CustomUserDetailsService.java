package com.fapshi.backend.security;

import com.fapshi.backend.entity.Client;
import com.fapshi.backend.entity.User;
import com.fapshi.backend.entity.Vendeur;
import com.fapshi.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

/**
 * Charge l'utilisateur et ses rôles pour valider le JWT et les autorisations.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Recherche par email ou téléphone
        User user = userRepository.findByEmail(username)
                .orElseGet(() -> userRepository.findByTelephone(username)
                        .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + username)));

        // Détermine le rôle à partir de l'entité (pas du token)
        String roleName = user instanceof Vendeur ? "VENDEUR" :
                          user instanceof Client ? "CLIENT" : "ADMIN";

        // Crée l'authority avec le préfixe ROLE_ (obligatoire pour hasRole)
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + roleName)
        );

        // Retourne UserDetails avec username, password et rôles
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }
}