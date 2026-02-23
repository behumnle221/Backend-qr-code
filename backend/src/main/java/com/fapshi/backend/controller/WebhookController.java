package com.fapshi.backend.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fapshi.backend.entity.QRCode;
import com.fapshi.backend.entity.Transaction;
import com.fapshi.backend.entity.Vendeur;
import com.fapshi.backend.entity.WebhookNotification;
import com.fapshi.backend.repository.QRCodeRepository;
import com.fapshi.backend.repository.TransactionRepository;
import com.fapshi.backend.repository.WebhookNotificationRepository;
import com.fapshi.backend.service.VendeurService;

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private QRCodeRepository qrCodeRepository;

    @Autowired
    private VendeurService vendeurService;

    @Autowired
    private WebhookNotificationRepository webhookNotificationRepository;

    // Map statique pour verrouiller le traitement par payToken
    private static final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    @PostMapping("/aangaraa")
    @Transactional
    public ResponseEntity<String> handleAangaraaWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader HttpHeaders headers) {

        // ============================================
        // 📝 LOGS DÉTAILLÉS POUR VÉRIFICATION
        // ============================================
        
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("🔔 WEBHOOK REÇU - HORODATAGE: {}", LocalDateTime.now());
        log.info("═══════════════════════════════════════════════════════════════");
        
        // Log des headers pour vérifier la source
        log.info("📋 EN-TÊTES REÇUS:");
        for (Map.Entry<String, String> entry : headers.toSingleValueMap().entrySet()) {
            log.info("   {}: {}", entry.getKey(), entry.getValue());
        }
        
        // Log du payload complet
        log.info("📦 PAYLOAD COMPLET REÇU:");
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            log.info("   {}: {}", entry.getKey(), entry.getValue());
        }
        
        // ============================================
        // SAUVEGARDER LA NOTIFICATION DANS LA BASE
        // ============================================
        
        String payToken = null;
        String status = null;
        
        try {
            WebhookNotification notification = new WebhookNotification();
            notification.setDateReception(LocalDateTime.now());
            
            payToken = (String) payload.getOrDefault("payToken", 
                         payload.getOrDefault("paytoken", 
                         payload.get("token")));
            
            status = (String) payload.get("status");
            String transactionIdExterne = (String) payload.get("transaction_id");
            
            notification.setPayToken(payToken);
            notification.setTransactionIdExterne(transactionIdExterne);
            notification.setMessage("Payload: " + payload.toString());
            
            if (status != null) {
                try {
                    notification.setStatus(com.fapshi.backend.enums.StatutTransaction.valueOf(status.toUpperCase()));
                } catch (Exception e) {
                    log.warn("⚠️ Statut non reconnu: {}", status);
                }
            }
            
            webhookNotificationRepository.save(notification);
            log.info("💾 Notification sauvegardée avec ID: {}", notification.getId());
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la sauvegarde de la notification: {}", e.getMessage());
        }
        
        // ============================================
        // TRAITEMENT DU WEBHOOK
        // ============================================
        
        try {
            if (payToken == null || status == null) {
                log.error("❌ Webhook invalide : payToken ou status manquant");
                log.error("🔍 Payload reçu : {}", payload);
                return ResponseEntity.ok("OK");
            }

            log.info("📝 PayToken: {}, Status: {}", payToken, status);

            // Normaliser le statut (AangaraaPay envoie SUCCESSFUL, FAILED, PENDING)
            String normalizedStatus = status.toUpperCase();
            
            // Création d'un lock pour ce payToken
            Object lock = locks.computeIfAbsent(payToken, k -> new Object());

            synchronized (lock) {
                Optional<Transaction> optTransaction = transactionRepository.findByPayToken(payToken);
                
                // Si pas trouvé par payToken, essayer par transaction_id
                if (optTransaction.isEmpty()) {
                    log.warn("⚠️ Transaction non trouvée pour payToken: {}", payToken);
                    log.warn("🔍 Recherche alternative par transaction_id dans le payload...");
                    Object transId = payload.get("transaction_id");
                    if (transId != null) {
                        // Essayer de trouver par transactionId (format: TRANS_1769339875485)
                        optTransaction = transactionRepository.findByTransactionId(transId.toString());
                        if (optTransaction.isEmpty()) {
                            // Sinon essayer par ID numérique
                            try {
                                Long tId = Long.parseLong(transId.toString());
                                optTransaction = transactionRepository.findById(tId);
                                if (optTransaction.isPresent()) {
                                    log.info("✅ Transaction trouvée par ID numérique: {}", tId);
                                }
                            } catch (NumberFormatException e) {
                                log.error("Impossible de parser transaction_id: {}", transId);
                            }
                        } else {
                            log.info("✅ Transaction trouvée par transactionId: {}", transId);
                        }
                    }
                    if (optTransaction.isEmpty()) {
                        log.error("❌ Transaction introuvable même avec recherche alternative");
                        return ResponseEntity.ok("OK");
                    }
                }

                Transaction transaction = optTransaction.get();
                log.info("✅ Transaction trouvée → ID: {} | Ancien statut: {} | PayToken: {}", 
                         transaction.getId(), transaction.getStatut(), transaction.getPayToken());

                // Éviter de traiter deux fois une transaction déjà réussie
                if ("SUCCESS".equals(transaction.getStatut())) {
                    log.info("⏭️ Transaction {} déjà traitée comme SUCCESS, ignorée", transaction.getId());
                    return ResponseEntity.ok("OK");
                }

                switch (normalizedStatus) {
                    case "SUCCESSFUL":
                    case "SUCCESS":
                        transaction.setStatut("SUCCESS");
                        handleSuccess(transaction);
                        log.info("✅ Transaction {} marquée comme SUCCESS", transaction.getId());
                        break;

                    case "FAILED":
                    case "CANCELLED":
                    case "CANCELED":
                        transaction.setStatut("FAILED");
                        log.info("❌ Transaction {} marquée comme FAILED", transaction.getId());
                        break;

                    case "PENDING":
                        log.info("⏳ Transaction {} reste PENDING", transaction.getId());
                        break;

                    default:
                        log.warn("⚠️ Statut inconnu reçu pour transaction {}: {}", transaction.getId(), normalizedStatus);
                        break;
                }

                transactionRepository.save(transaction);
                log.info("✅ Webhook traité → Transaction {} → {}", transaction.getId(), transaction.getStatut());
            }

            // Supprime le lock après traitement pour éviter fuite mémoire
            locks.remove(payToken);

            log.info("═══════════════════════════════════════════════════════════════");
            log.info("✅ FIN DU TRAITEMENT WEBHOOK");
            log.info("═══════════════════════════════════════════════════════════════");
            
            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("❌ Erreur critique dans le webhook", e);
            return ResponseEntity.ok("OK");
        }
    }

    private void handleSuccess(Transaction transaction) {
        try {
            log.info("🔄 Début du traitement handleSuccess pour transaction {}", transaction.getId());
            
            // 1. Marquer le QR code comme utilisé
            QRCode qrCode = transaction.getQrCode();
            if (qrCode != null) {
                qrCode.setEstUtilise(true);
                qrCodeRepository.save(qrCode);
                log.info("✅ QR Code {} marqué comme utilisé", qrCode.getId());
            } else {
                log.warn("⚠️ QR Code null pour transaction {}", transaction.getId());
            }

            // 2. Augmenter le solde du vendeur
            Vendeur vendeur = qrCode != null ? qrCode.getVendeur() : null;
            if (vendeur != null) {
                BigDecimal montantNet = transaction.getMontantNet() != null ? transaction.getMontantNet() : transaction.getMontant();
                log.info("💰 Montant net à créditer: {}", montantNet);
                log.info("💰 Solde actuel du vendeur {}: {}", vendeur.getId(), vendeur.getSoldeVirtuel());
                
                vendeurService.augmenterSolde(vendeur.getId(), montantNet);
                
                log.info("💰 Solde vendeur {} augmenté de {}. Nouveau solde: {}", 
                    vendeur.getId(), montantNet, 
                    findVendeurById(vendeur.getId()).map(v -> v.getSoldeVirtuel()).orElse(BigDecimal.ZERO));
            } else {
                log.error("❌ Vendeur null pour transaction {}", transaction.getId());
            }
            
            log.info("✅ Fin du traitement handleSuccess pour transaction {}", transaction.getId());
        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement SUCCESS pour transaction {}: {}", transaction.getId(), e.getMessage(), e);
        }
    }
    
    // Methode helper pour récupérer le vendeur mis à jour
    private Optional<Vendeur> findVendeurById(Long id) {
        return vendeurService.findById(id);
    }


    @PostMapping("/test-aangaraa")
    public ResponseEntity<String> testWebhook(@RequestParam Long transactionId, @RequestParam String status) {
        log.info("🔧 Test Webhook → transactionId: {} | status: {}", transactionId, status);
        Transaction transaction = transactionRepository.findById(transactionId).orElse(null);
        if (transaction != null) {
            transaction.setStatut(status.toUpperCase());
            transactionRepository.save(transaction);
            log.info("✅ Transaction {} mise à jour pour test", transactionId);
            return ResponseEntity.ok("Test OK");
        } else {
            return ResponseEntity.badRequest().body("Transaction non trouvée");
        }
    }
    
    // ============================================
    // ENDPOINT POUR VOIR LES NOTIFICATIONS REÇUES
    // ============================================
    @PostMapping("/notifications/list")
    public ResponseEntity<?> getWebhookNotifications() {
        try {
            var notifications = webhookNotificationRepository.findAll();
            log.info("📋 Nombre de notifications reçues: {}", notifications.size());
            
            // Retourner sous forme simplifiée
            java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (WebhookNotification n : notifications) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", n.getId());
                item.put("payToken", n.getPayToken());
                item.put("status", n.getStatus());
                item.put("dateReception", n.getDateReception());
                item.put("traite", n.isTraite());
                item.put("transactionIdExterne", n.getTransactionIdExterne());
                result.add(item);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des notifications: {}", e.getMessage());
            return ResponseEntity.status(500).body("Erreur: " + e.getMessage());
        }
    }
} 
