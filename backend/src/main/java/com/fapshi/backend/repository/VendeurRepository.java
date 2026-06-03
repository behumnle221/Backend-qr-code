package com.fapshi.backend.repository;

import com.fapshi.backend.entity.Vendeur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Repository pour les Vendeurs.
 * Utile pour récupérer un vendeur par son téléphone ou ID.
 */
@Repository
public interface VendeurRepository extends JpaRepository<Vendeur, Long> {

    // Recherche un vendeur par son numéro de téléphone
    Optional<Vendeur> findByTelephone(String telephone);
    
    // Recherche par email
    Optional<Vendeur> findByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE Vendeur v SET v.soldeVirtuel = v.soldeVirtuel + :montant WHERE v.id = :id")
    int crediterVendeur(@Param("id") Long id, @Param("montant") BigDecimal montant);
}

