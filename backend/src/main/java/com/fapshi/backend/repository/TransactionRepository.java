package com.fapshi.backend.repository;

import com.fapshi.backend.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByQrCodeVendeurId(Long vendeurId);
    List<Transaction> findByClientId(Long clientId);
    List<Transaction> findByStatut(String statut);
    
    // Query pour récupérer les transactions d'un vendeur avec pagination et filtres
    @Query("SELECT t FROM Transaction t " +
           "WHERE t.qrCode.vendeur.id = :vendeurId " +
           "AND (:statut IS NULL OR t.statut = :statut) " +
           "AND (:dateDebut IS NULL OR t.dateCreation >= :dateDebut) " +
           "AND (:dateFin IS NULL OR t.dateCreation <= :dateFin) " +
           "ORDER BY t.dateCreation DESC")
    Page<Transaction> findTransactionsByVendeur(
        @Param("vendeurId") Long vendeurId,
        @Param("statut") String statut,
        @Param("dateDebut") LocalDateTime dateDebut,
        @Param("dateFin") LocalDateTime dateFin,
        Pageable pageable
    );



    // AJOUT : Query pour récupérer les transactions d'un client avec pagination et filtres
    @Query("SELECT t FROM Transaction t " +
           "WHERE t.client.id = :clientId " +
           "AND (:statut IS NULL OR t.statut = :statut) " +
           "AND (:dateDebut IS NULL OR t.dateCreation >= :dateDebut) " +
           "AND (:dateFin IS NULL OR t.dateCreation <= :dateFin) " +
           "ORDER BY t.dateCreation DESC")
    Page<Transaction> findTransactionsByClient(
        @Param("clientId") Long clientId,
        @Param("statut") String statut,
        @Param("dateDebut") LocalDateTime dateDebut,
        @Param("dateFin") LocalDateTime dateFin,
        Pageable pageable
    );

    
    // Recherche d'une transaction par son payToken (utilisé pour les webhooks)
    Optional<Transaction> findByPayToken(String payToken);
}