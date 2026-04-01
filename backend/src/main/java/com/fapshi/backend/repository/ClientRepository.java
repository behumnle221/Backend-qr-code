package com.fapshi.backend.repository;

import com.fapshi.backend.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Repository spécifique pour les Clients.
 * Hérite de toutes les méthodes CRUD de base.
 */
@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByTelephone(String telephone);

    @Modifying
    @Transactional
    @Query("UPDATE Client c SET c.soldeVirtuel = c.soldeVirtuel - :montant " +
           "WHERE c.id = :id AND c.soldeVirtuel >= :montant")
    int debiterSiSuffisant(@Param("id") Long id, @Param("montant") BigDecimal montant);
}