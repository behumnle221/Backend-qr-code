package com.fapshi.backend.repository;

import com.fapshi.backend.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository pour l'entité Notification
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Récupère toutes les notifications d'un vendeur paginées
     */
    @Query("SELECT n FROM Notification n WHERE n.vendeur.id = :vendeurId ORDER BY n.dateCreation DESC")
    Page<Notification> findByVendeurId(Long vendeurId, Pageable pageable);

    /**
     * Compte les notifications non lues d'un vendeur
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.vendeur.id = :vendeurId AND n.lue = false")
    long countUnreadByVendeurId(Long vendeurId);

    /**
     * Récupère les notifications non lues d'un vendeur
     */
    @Query("SELECT n FROM Notification n WHERE n.vendeur.id = :vendeurId AND n.lue = false ORDER BY n.dateCreation DESC")
    Page<Notification> findUnreadByVendeurId(Long vendeurId, Pageable pageable);
}
