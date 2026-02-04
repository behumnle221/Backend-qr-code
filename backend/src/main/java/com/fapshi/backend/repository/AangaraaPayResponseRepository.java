package com.fapshi.backend.repository;

import com.fapshi.backend.entity.AangaraaPayResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository pour les réponses de l'API Aangaraa-Pay.
 */
@Repository
public interface AangaraaPayResponseRepository extends JpaRepository<AangaraaPayResponse, Long> {
}