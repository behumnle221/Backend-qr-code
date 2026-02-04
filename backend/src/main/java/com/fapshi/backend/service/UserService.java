package com.fapshi.backend.service;

import com.fapshi.backend.entity.User;
import com.fapshi.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service pour gérer les opérations sur l'entité User (classe mère).
 * Utilisé pour l'inscription, la connexion, la recherche par email/téléphone.
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Sauvegarde un utilisateur (inscription)
    public User save(User user) {
        return userRepository.save(user);
    }

    // Recherche par ID
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    // Tous les utilisateurs
    public List<User> findAll() {
        return userRepository.findAll();
    }

    // Recherche par email
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Recherche par téléphone
    public Optional<User> findByTelephone(String telephone) {
        return userRepository.findByTelephone(telephone);
    }

    // Vérifie si email existe déjà
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // Vérifie si téléphone existe déjà
    public boolean existsByTelephone(String telephone) {
        return userRepository.existsByTelephone(telephone);
    }
}