package com.fapshi.backend.repository;

import com.fapshi.backend.entity.WebhookNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour les notifications webhook reçues.
 * Utile pour le retry des notifications non traitées.
 */
@Repository
public interface WebhookNotificationRepository extends JpaRepository<WebhookNotification, Long> {

    // Toutes les notifications pour un payToken
    List<WebhookNotification> findByPayToken(String payToken);

   

    // Notifications non encore traitées (pour retry)
    List<WebhookNotification> findByTraiteFalse();
}