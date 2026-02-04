package com.fapshi.backend.repository;

import com.fapshi.backend.entity.ApiCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour les clés API (appKey Aangaraa-Pay).
 */
@Repository
public interface ApiCredentialsRepository extends JpaRepository<ApiCredentials, Long> {

    // Recherche par la clé appKey
    Optional<ApiCredentials> findByAppKey(String appKey);
}