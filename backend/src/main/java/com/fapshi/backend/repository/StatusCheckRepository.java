package com.fapshi.backend.repository;

import com.fapshi.backend.entity.StatusCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository pour l'historique des vérifications de statut (polling).
 */
@Repository
public interface StatusCheckRepository extends JpaRepository<StatusCheck, Long> {
}