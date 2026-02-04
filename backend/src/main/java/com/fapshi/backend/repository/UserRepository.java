package com.fapshi.backend.repository;

import com.fapshi.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour l'entité User (classe mère).
 * Permet de faire des opérations CRUD sur la table users.
 * Méthodes personnalisées pour chercher par email ou téléphone.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Recherche un utilisateur par son email (retourne Optional pour gérer le cas "non trouvé")
    Optional<User> findByEmail(String email);

    // Recherche un utilisateur par son téléphone
    Optional<User> findByTelephone(String telephone);

    // Vérifie si un email existe déjà (utile pour l'inscription)
    boolean existsByEmail(String email);

    // Vérifie si un téléphone existe déjà
    boolean existsByTelephone(String telephone);
}