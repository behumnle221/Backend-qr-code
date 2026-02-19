package com.fapshi.backend.service;

import com.fapshi.backend.dto.response.NotificationResponse;
import com.fapshi.backend.entity.Notification;
import com.fapshi.backend.entity.Vendeur;
import com.fapshi.backend.repository.NotificationRepository;
import com.fapshi.backend.repository.VendeurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service pour gérer les notifications
 * Simulation Pusher : les notifications sont stockées en DB et logs
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private VendeurRepository vendeurRepository;

    /**
     * Crée et envoie une notification (simulation Pusher)
     * 
     * @param vendeurId ID du vendeur
     * @param type Type de notification (RETRAIT_DEMANDE, RETRAIT_SUCCESS, etc.)
     * @param titre Titre de la notification
     * @param message Message de la notification
     * @return NotificationResponse créée
     */
    public NotificationResponse creerNotification(Long vendeurId, String type, String titre, String message) {
        Vendeur vendeur = vendeurRepository.findById(vendeurId)
                .orElseThrow(() -> new RuntimeException("Vendeur non trouvé"));

        // Créer la notification en DB
        Notification notification = new Notification(vendeur, type, titre, message);
        notification = notificationRepository.save(notification);

        // Simuler envoi Pusher (en réalité, c'est juste un log)
        this.envoyerPusherNotification(vendeurId, type, titre, message);

        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitre(),
                notification.getMessage(),
                notification.getDateCreation(),
                notification.isLue()
        );
    }

    /**
     * Simule l'envoi d'une notification via Pusher
     * En production, cela ferait un appel HTTP vers Pusher
     * 
     * @param vendeurId ID du vendeur
     * @param type Type de notification
     * @param titre Titre
     * @param message Message
     */
    private void envoyerPusherNotification(Long vendeurId, String type, String titre, String message) {
        // 🔔 Simulation Pusher : on log simplement
        log.info("🔔 [PUSHER SIMULATION] Notification envoyée au vendeur {} - Type: {} - Titre: {} - Message: {}", 
                vendeurId, type, titre, message);
    }

    /**
     * Récupère les notifications d'un vendeur (paginé)
     */
    public Page<NotificationResponse> getNotifications(Long vendeurId, Pageable pageable) {
        Page<Notification> notifs = notificationRepository.findByVendeurId(vendeurId, pageable);
        return notifs.map(n -> new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitre(),
                n.getMessage(),
                n.getDateCreation(),
                n.isLue()
        ));
    }

    /**
     * Récupère le nombre de notifications non lues
     */
    public long countUnread(Long vendeurId) {
        return notificationRepository.countUnreadByVendeurId(vendeurId);
    }

    /**
     * Marque une notification comme lue
     */
    public void markAsRead(Long notificationId) {
        Notification notif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée"));

        notif.setLue(true);
        notificationRepository.save(notif);
        log.info("✅ Notification {} marquée comme lue", notificationId);
    }

    /**
     * Marque toutes les notifications d'un vendeur comme lues
     */
    public void markAllAsRead(Long vendeurId) {
        Vendeur vendeur = vendeurRepository.findById(vendeurId)
                .orElseThrow(() -> new RuntimeException("Vendeur non trouvé"));

        // Cherche toutes les notifications non lues et les marquer comme lues
        for (int page = 0; page < Integer.MAX_VALUE; page++) {
            Page<Notification> page_notif = notificationRepository.findUnreadByVendeurId(vendeurId, 
                    org.springframework.data.domain.PageRequest.of(page, 100));
            
            if (page_notif.isEmpty()) break;

            for (Notification n : page_notif.getContent()) {
                n.setLue(true);
                notificationRepository.save(n);
            }
        }

        log.info("✅ Toutes les notifications du vendeur {} marquées comme lues", vendeurId);
    }
}
