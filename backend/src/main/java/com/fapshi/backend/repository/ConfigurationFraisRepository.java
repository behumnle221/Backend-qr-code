package com.fapshi.backend.repository;

import com.fapshi.backend.entity.ConfigurationFrais;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository pour la configuration des frais (une seule ligne en général).
 */
@Repository
public interface ConfigurationFraisRepository extends JpaRepository<ConfigurationFrais, Long> {
}