package com.fapshi.backend.repository;

import com.fapshi.backend.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository spécifique pour les Clients.
 * Hérite de toutes les méthodes CRUD de base.
 */
@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    // Pas de méthodes personnalisées pour l'instant (on peut en ajouter plus tard)
}