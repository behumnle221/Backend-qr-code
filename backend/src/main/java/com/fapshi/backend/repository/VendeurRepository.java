package com.fapshi.backend.repository;

import com.fapshi.backend.entity.Vendeur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour les Vendeurs.
 * Utile pour récupérer un vendeur par son téléphone ou ID.
 */
@Repository
public interface VendeurRepository extends JpaRepository<Vendeur, Long> {

    // Recherche un vendeur par son numéro de téléphone
    Optional<Vendeur> findByTelephone(String telephone);
    
    // ⬅️ NOUVELLE MÉTHODE AJOUTÉE pour la recherche par email
    Optional<Vendeur> findByEmail(String email);
}

