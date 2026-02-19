package com.fapshi.backend.controller;

import com.fapshi.backend.entity.QRCode;
import com.fapshi.backend.entity.Transaction;
import com.fapshi.backend.entity.Vendeur;
import com.fapshi.backend.repository.QRCodeRepository;
import com.fapshi.backend.repository.TransactionRepository;
import com.fapshi.backend.service.VendeurService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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

    // Map statique pour verrouiller le traitement par payToken
    private static final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    @PostMapping("/aangaraa")
    @Transactional
    public ResponseEntity<String> handleAangaraaWebhook(@RequestBody Map<String, Object> payload) {

        log.info("🔔 WEBHOOK REÇU D'AANGARAA");
        log.info("Payload complet : {}", payload);

        try {
            String payToken = (String) payload.getOrDefault("payToken", payload.get("paytoken"));
            String status   = (String) payload.get("status");

            if (payToken == null || status == null) {
                log.error("❌ Webhook invalide : payToken ou status manquant");
                return ResponseEntity.ok("OK");
            }

            // Création d’un lock pour ce payToken
            Object lock = locks.computeIfAbsent(payToken, k -> new Object());

            synchronized (lock) {
                Optional<Transaction> optTransaction = transactionRepository.findByPayToken(payToken);
                if (optTransaction.isEmpty()) {
                    log.warn("⚠️ Transaction non trouvée pour payToken: {}", payToken);
                    return ResponseEntity.ok("OK");
                }

                Transaction transaction = optTransaction.get();
                log.info("✅ Transaction trouvée → ID: {} | Ancien statut: {}", 
                         transaction.getId(), transaction.getStatut());

                status = status.toUpperCase();

                switch (status) {
                    case "SUCCESSFUL":
                        if (!"SUCCESS".equals(transaction.getStatut())) {
                            transaction.setStatut("SUCCESS");
                            handleSuccess(transaction);
                        }
                        break;

                    case "FAILED":
                    case "CANCELLED":
                        if (!"FAILED".equals(transaction.getStatut())) {
                            transaction.setStatut("FAILED");
                        }
                        break;

                    case "PENDING":
                        log.info("Transaction {} reste PENDING", transaction.getId());
                        break;

                    default:
                        log.warn("Statut inconnu reçu pour transaction {}: {}", transaction.getId(), status);
                        break;
                }

                transactionRepository.save(transaction);
                log.info("✅ Webhook traité → Transaction {} → {}", transaction.getId(), transaction.getStatut());
            }

            // Supprime le lock après traitement pour éviter fuite mémoire
            locks.remove(payToken);

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("❌ Erreur critique dans le webhook", e);
            return ResponseEntity.ok("OK");
        }
    }

    private void handleSuccess(Transaction transaction) {
        try {
            QRCode qrCode = transaction.getQrCode();
            if (qrCode != null) {
                qrCode.setEstUtilise(true);
                qrCodeRepository.save(qrCode);
            }

            Vendeur vendeur = qrCode != null ? qrCode.getVendeur() : null;
            if (vendeur != null) {
                BigDecimal montantNet = transaction.getMontantNet() != null ? transaction.getMontantNet() : transaction.getMontant();
                vendeurService.augmenterSolde(vendeur.getId(), montantNet);
                log.info("💰 Solde vendeur {} augmenté de {}", vendeur.getId(), montantNet);
            }
        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement SUCCESS pour transaction {}: {}", transaction.getId(), e.getMessage());
        }
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
}


