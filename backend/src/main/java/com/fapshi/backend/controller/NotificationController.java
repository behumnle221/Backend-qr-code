package com.fapshi.backend.controller;

import com.fapshi.backend.dto.request.NotificationSendRequest;
import com.fapshi.backend.dto.response.ApiResponse;
import com.fapshi.backend.dto.response.NotificationResponse;
import com.fapshi.backend.entity.Vendeur;
import com.fapshi.backend.service.NotificationService;
import com.fapshi.backend.service.VendeurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller pour les notifications
 * Endpoints protégés : authentification JWT + rôle VENDEUR requis
 */
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private VendeurService vendeurService;

    /**
     * Envoie une notification de test (simulation Pusher)
     * Endpoint : POST /api/notification/send
     * Authentification : JWT + rôle VENDEUR
     * 
     * Body:
     * {
     *   "titre": "Test",
     *   "message": "Ceci est un test",
     *   "type": "TEST"
     * }
     * 
     * Réponse (Code 201):
     * {
     *   "success": true,
     *   "message": "Notification envoyée",
     *   "data": { ... }
     * }
     */
    @PostMapping("/send")
    @PreAuthorize("hasAuthority('ROLE_VENDEUR')")
    @Operation(summary = "Envoyer une notification de test", description = "Crée et envoie une notification via simulation Pusher")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(@RequestBody NotificationSendRequest request) {
        try {
            // Récupérer le vendeur connecté
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            Vendeur vendeur = vendeurService.findByEmail(username)
                    .or(() -> vendeurService.findByTelephone(username))
                    .orElseThrow(() -> new RuntimeException("Vendeur non trouvé"));

            // Créer et envoyer la notification
            NotificationResponse notif = notificationService.creerNotification(
                    vendeur.getId(),
                    request.getType(),
                    request.getTitre(),
                    request.getMessage()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<NotificationResponse>(true, "Notification envoyée", notif));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<NotificationResponse>(false, "Erreur: " + e.getMessage(), null));
        }
    }

    /**
     * Récupère les notifications du vendeur connecté (paginées)
     * Endpoint : GET /api/notification?page=0&size=10
     * Authentification : JWT + rôle VENDEUR
     * 
     * Réponse (Code 200):
     * {
     *   "success": true,
     *   "message": "Notifications récupérées",
     *   "data": {
     *     "content": [ ... ],
     *     "totalElements": 5,
     *     "totalPages": 1,
     *     "currentPage": 0,
     *     "pageSize": 10,
     *     "unreadCount": 2
     *   }
     * }
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_VENDEUR')")
    @Operation(summary = "Récupérer les notifications", description = "Récupère les notifications du vendeur connecté (paginées)")
    public ResponseEntity<ApiResponse<Object>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            // Récupérer le vendeur connecté
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            Vendeur vendeur = vendeurService.findByEmail(username)
                    .or(() -> vendeurService.findByTelephone(username))
                    .orElseThrow(() -> new RuntimeException("Vendeur non trouvé"));

            // Récupérer les notifications paginées
            Pageable pageable = PageRequest.of(page, size);
            Page<NotificationResponse> notifs = notificationService.getNotifications(vendeur.getId(), pageable);

            // Formatter la réponse
            Map<String, Object> responseData = new LinkedHashMap<>();
            responseData.put("content", notifs.getContent());
            responseData.put("totalElements", notifs.getTotalElements());
            responseData.put("totalPages", notifs.getTotalPages());
            responseData.put("currentPage", notifs.getNumber());
            responseData.put("pageSize", notifs.getSize());
            responseData.put("hasNextPage", notifs.hasNext());
            responseData.put("hasPreviousPage", notifs.hasPrevious());
            responseData.put("unreadCount", notificationService.countUnread(vendeur.getId()));

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(true, "Notifications récupérées", responseData));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Erreur: " + e.getMessage(), null));
        }
    }

    /**
     * Marque une notification comme lue
     * Endpoint : PUT /api/notification/{id}/read
     * Authentification : JWT + rôle VENDEUR
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("hasAuthority('ROLE_VENDEUR')")
    @Operation(summary = "Marquer une notification comme lue", description = "Marque une notification spécifique comme lue")
    public ResponseEntity<ApiResponse<Map<String, String>>> markAsRead(@PathVariable Long id) {
        try {
            notificationService.markAsRead(id);
            return ResponseEntity.ok()
                    .body(new ApiResponse<>(true, "Notification marquée comme lue", 
                            Map.of("notificationId", id.toString())));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("non trouvée")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Notification non trouvée", null));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Erreur: " + e.getMessage(), null));
        }
    }

    /**
     * Marque toutes les notifications comme lues
     * Endpoint : PUT /api/notification/read-all
     * Authentification : JWT + rôle VENDEUR
     */
    @PutMapping("/read-all")
    @PreAuthorize("hasAuthority('ROLE_VENDEUR')")
    @Operation(summary = "Marquer toutes les notifications comme lues", description = "Marque toutes les notifications du vendeur comme lues")
    public ResponseEntity<ApiResponse<Map<String, String>>> markAllAsRead() {
        try {
            // Récupérer le vendeur connecté
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            Vendeur vendeur = vendeurService.findByEmail(username)
                    .or(() -> vendeurService.findByTelephone(username))
                    .orElseThrow(() -> new RuntimeException("Vendeur non trouvé"));

            notificationService.markAllAsRead(vendeur.getId());
            return ResponseEntity.ok()
                    .body(new ApiResponse<>(true, "Toutes les notifications marquées comme lues", 
                            Map.of("vendeurId", vendeur.getId().toString())));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Erreur: " + e.getMessage(), null));
        }
    }
}
