package com.fapshi.backend.repository;

import com.fapshi.backend.entity.AangaraaPayRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour les requêtes envoyées à l'API Aangaraa-Pay.
 */
@Repository
public interface AangaraaPayRequestRepository extends JpaRepository<AangaraaPayRequest, Long> {

    // Recherche par l'ID externe renvoyé par Aangaraa
    Optional<AangaraaPayRequest> findByExternalTransactionId(String externalTransactionId);
}