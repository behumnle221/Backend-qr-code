package com.fapshi.backend.controller;

import com.fapshi.backend.dto.response.ApiResponse;
import com.fapshi.backend.entity.QRCode;
import com.fapshi.backend.entity.Transaction;
import com.fapshi.backend.entity.Vendeur;
import com.fapshi.backend.repository.QRCodeRepository;
import com.fapshi.backend.repository.TransactionRepository;
import com.fapshi.backend.service.VendeurService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Controller pour traiter les webhooks d'Aangaraa Pay
 * Ces endpoints ne sont PAS protégés par JWT (Aangaraa appelle depuis l'extérieur)
 */
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

    /**
     * Endpoint webhook pour recevoir les notifications d'Aangaraa Pay
     * URL : POST /api/webhook/aangaraa
     * 
     * Payload attendu d'Aangaraa (exemple) :
     * {
     *   "payToken": "abc123def456",
     *   "status": "SUCCESSFUL" ou "FAILED" ou "PENDING",
     *   "transaction_id": "12345",
     *   "message": "Payment processed successfully"
     * }
     */
    @Operation(
        summary = "Webhook Aangaraa Pay",
        description = "Reçoit les notifications de statut de paiement d'Aangaraa Pay"
    )
    @PostMapping("/aangaraa")
    public ResponseEntity<ApiResponse<Map<String, String>>> handleAangaraaWebhook(
            @RequestBody Map<String, Object> payload) {
        
        try {
            log.info("🔔 Webhook reçu d'Aangaraa : {}", payload);
            
            // Récupérer les champs du webhook
            String payToken = (String) payload.get("payToken");
            String status = (String) payload.get("status");
            String transactionIdStr = (String) payload.get("transaction_id");
            String message = (String) payload.get("message");
            
            // Valider les champs obligatoires
            if (payToken == null || status == null) {
                log.error("❌ Webhook invalide : payToken ou status manquant");
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>("payToken et status sont obligatoires"));
            }
            
            // Chercher la transaction par payToken
            Optional<Transaction> transactionOpt = transactionRepository.findAll().stream()
                    .filter(tx -> payToken.equals(tx.getPayToken()))
                    .findFirst();
            
            if (transactionOpt.isEmpty()) {
                log.warn("⚠️ Transaction non trouvée pour payToken: {}", payToken);
                return ResponseEntity.ok()
                        .body(new ApiResponse<>("Transaction non trouvée (peut avoir été traitée)"));
            }
            
            Transaction transaction = transactionOpt.get();
            log.info("✅ Transaction trouvée : ID {}, statut actuel: {}", transaction.getId(), transaction.getStatut());
            
            // Mettre à jour le statut de la transaction selon le webhook
            if ("SUCCESSFUL".equalsIgnoreCase(status)) {
                transaction.setStatut("SUCCESS");
                
                // Marquer le QR code comme utilisé
                QRCode qrCode = transaction.getQrCode();
                if (qrCode != null) {
                    qrCode.setEstUtilise(true);
                    qrCodeRepository.save(qrCode);
                    log.info("✅ QR Code {} marqué comme utilisé", qrCode.getId());
                }
                
                // 🔶 METTRE À JOUR LE SOLDE DU VENDEUR
                Vendeur vendeur = qrCode.getVendeur();
                if (vendeur != null) {
                    vendeurService.augmenterSolde(vendeur.getId(), transaction.getMontantNet());
                    log.info("💰 Solde vendeur {} augmenté de {} XAF", vendeur.getId(), transaction.getMontantNet());
                }
                
            } else if ("FAILED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
                transaction.setStatut("FAILED");
                log.warn("❌ Paiement échoué/annulé : {}", message);
                
            } else if ("PENDING".equalsIgnoreCase(status)) {
                transaction.setStatut("PENDING");
                log.info("⏳ Paiement en attente");
            } else {
                transaction.setStatut(status);
                log.info("ℹ️ Statut reçu : {}", status);
            }
            
            // Sauvegarder la transaction
            transactionRepository.save(transaction);
            
            log.info("✅ Webhook traité avec succès pour transaction {}", transaction.getId());
            
            return ResponseEntity.ok()
                    .body(new ApiResponse<>(
                            Map.of(
                                    "success", "true",
                                    "transactionId", transaction.getId().toString(),
                                    "statut", transaction.getStatut()
                            ),
                            "Webhook traité avec succès"
                    ));
            
        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement du webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Erreur: " + e.getMessage()));
        }
    }

    /**
     * Endpoint de test pour simuler un webhook (utile pour les tests sans Aangaraa réel)
     * URL : POST /api/webhook/test-aangaraa
     */
    @Operation(
        summary = "Test webhook Aangaraa",
        description = "Simule un webhook Aangaraa (pour les tests)"
    )
    @PostMapping("/test-aangaraa")
    public ResponseEntity<ApiResponse<Map<String, String>>> testWebhook(
            @RequestParam Long transactionId,
            @RequestParam String status) {
        
        try {
            log.info("🧪 Test webhook : transactionId={}, status={}", transactionId, status);
            
            Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
            if (transactionOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>("Transaction non trouvée"));
            }
            
            Transaction transaction = transactionOpt.get();
            transaction.setStatut(status);
            
            if ("SUCCESS".equalsIgnoreCase(status)) {
                QRCode qrCode = transaction.getQrCode();
                if (qrCode != null) {
                    qrCode.setEstUtilise(true);
                    qrCodeRepository.save(qrCode);
                }
                
                Vendeur vendeur = qrCode.getVendeur();
                if (vendeur != null) {
                    vendeurService.augmenterSolde(vendeur.getId(), transaction.getMontantNet());
                }
            }
            
            transactionRepository.save(transaction);
            
            return ResponseEntity.ok()
                    .body(new ApiResponse<>(
                            Map.of(
                                    "success", "true",
                                    "transactionId", transaction.getId().toString(),
                                    "statut", transaction.getStatut()
                            ),
                            "Test webhook exécuté"
                    ));
            
        } catch (Exception e) {
            log.error("Erreur test webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Erreur: " + e.getMessage()));
        }
    }
}
