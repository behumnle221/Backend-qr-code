package com.fapshi.backend.repository;

import com.fapshi.backend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour les Transactions.
 * Permet de récupérer l'historique par client ou vendeur.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Toutes les transactions d'un client
    List<Transaction> findByClientId(Long clientId);

    // Toutes les transactions reçues par un vendeur
    List<Transaction> findByVendeurId(Long vendeurId);
}