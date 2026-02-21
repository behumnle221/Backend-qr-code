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
            // Support de plusieurs formats de champs (payToken, paytoken, token)
            String payToken = (String) payload.getOrDefault("payToken", 
                         payload.getOrDefault("paytoken", 
                         payload.get("token")));
            String status   = (String) payload.get("status");

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
                        try {
                            Long tId = Long.parseLong(transId.toString());
                            optTransaction = transactionRepository.findById(tId);
                            if (optTransaction.isPresent()) {
                                log.info("✅ Transaction trouvée par transaction_id: {}", tId);
                            }
                        } catch (NumberFormatException e) {
                            log.error("Impossible de parser transaction_id: {}", transId);
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
}
