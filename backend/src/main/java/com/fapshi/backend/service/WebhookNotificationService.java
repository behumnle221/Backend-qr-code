package com.fapshi.backend.service;

import com.fapshi.backend.entity.WebhookNotification;
import com.fapshi.backend.repository.WebhookNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service pour traiter les notifications webhook (temps réel).
 */
@Service
public class WebhookNotificationService {

    @Autowired
    private WebhookNotificationRepository notificationRepository;

    public WebhookNotification save(WebhookNotification notification) {
        return notificationRepository.save(notification);
    }

    public List<WebhookNotification> findByPayToken(String payToken) {
        return notificationRepository.findByPayToken(payToken);
    }

    // Récupère les notifications non traitées (pour retry)
    public List<WebhookNotification> findNonTraitees() {
        return notificationRepository.findByTraiteFalse();
    }
}