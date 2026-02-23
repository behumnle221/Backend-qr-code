package com.fapshi.backend.service;

import com.fapshi.backend.dto.external.AangaraaPaymentResponse;
import com.fapshi.backend.dto.request.InitiatePaymentRequest;
import com.fapshi.backend.dto.response.PaymentInitResponse;
import com.fapshi.backend.entity.ConfigurationFrais;
import com.fapshi.backend.entity.QRCode;
import com.fapshi.backend.entity.Transaction;
import com.fapshi.backend.entity.Vendeur;
import com.fapshi.backend.repository.ConfigurationFraisRepository;
import com.fapshi.backend.repository.QRCodeRepository;
import com.fapshi.backend.repository.TransactionRepository;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Autowired private QRCodeRepository qrCodeRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private ConfigurationFraisRepository configurationFraisRepository;
    @Autowired private VendeurService vendeurService;
    @Autowired private RestTemplate restTemplate;

    @Value("${app.aangaraa.webhook-url:}")
    private String webhookUrl;

    // Fallback URL si variable d'environnement non définie
    private String getWebhookUrl() {
        // D'abord vérifier la variable d'environnement AANGARAA_WEBHOOK_URL
        String envWebhookUrl = System.getenv("AANGARAA_WEBHOOK_URL");
        if (envWebhookUrl != null && !envWebhookUrl.isEmpty()) {
            log.info("🔗 Utilisation de AANGARAA_WEBHOOK_URL: {}", envWebhookUrl);
            return envWebhookUrl;
        }
        
        // Fallback: vérifier la propriété Spring
        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            return webhookUrl;
        }
        
        // Dernier fallback: utiliser RENDER_EXTERNAL_URL
        String renderUrl = System.getenv("RENDER_EXTERNAL_URL");
        if (renderUrl != null && !renderUrl.isEmpty()) {
            return renderUrl + "/api/webhook/aangaraa";
        }
        
        // URL de secours
        return "https://backend-qr-code-u2kx.onrender.com/api/webhook/aangaraa";
    }

    private static final String APP_KEY = "NRYT-9742-EHQY-QB4B";
    private static final String URL_DIRECT   = "https://api-production.aangaraa-pay.com/api/v1/no_redirect/payment";
    private static final String URL_REDIRECT = "https://api-production.aangaraa-pay.com/api/v1/redirect/payment";
    private static final String URL_CHECK    = "https://api-production.aangaraa-pay.com/api/v1/aangaraa_check_status";

    /**
     * Étape 1 : Initialisation du paiement (Mobile -> Backend)
     */
    @Transactional
    public PaymentInitResponse initiatePayment(InitiatePaymentRequest request) {
        validateRequest(request);

        QRCode qrCode = qrCodeRepository.findById(request.getQrCodeId())
                .orElseThrow(() -> new RuntimeException("QR Code non trouvé"));

        if (qrCode.isEstUtilise()) throw new RuntimeException("QR Code déjà payé.");
        if (qrCode.getDateExpiration().isBefore(LocalDateTime.now())) throw new RuntimeException("QR Code expiré.");

        // Création de la transaction en base de données
        Transaction transaction = new Transaction();
        transaction.setQrCode(qrCode);
        transaction.setTelephoneClient(request.getTelephoneClient());
        transaction.setMontant(request.getMontant());
        transaction.setOperator(request.getOperator());
        transaction.setStatut("PENDING");
        transaction.setDateCreation(LocalDateTime.now());
        
        // Générer le transactionId au format TRANS_1769339875485 (TRANS_timestamp)
        long timestamp = System.currentTimeMillis();
        transaction.setTransactionId("TRANS_" + timestamp);

        calculateCommissionAndNetAmount(transaction);
        transaction = transactionRepository.save(transaction);

        // Préparation de l'appel vers Aangaraa
        String url = request.isDirectPayment() ? URL_DIRECT : URL_REDIRECT;
        Map<String, Object> payload = prepareAangaraaPayload(request, qrCode, transaction);

        try {
            log.info("Appel Aangaraa URL: {} | Transaction ID: {}", url, transaction.getId());
            AangaraaPaymentResponse apiResponse = restTemplate.postForObject(url, payload, AangaraaPaymentResponse.class);

            if (apiResponse == null || apiResponse.getData() == null) {
                throw new RuntimeException("Réponse vide de l'API Aangaraa");
            }

            AangaraaPaymentResponse.Data data = apiResponse.getData();
            transaction.setPayToken(data.getPayToken());
            transaction.setPayUrl(data.getPayment_url());
            transaction.setReferenceOperateur(data.getTransaction_id());
            transactionRepository.save(transaction);

            PaymentInitResponse response = new PaymentInitResponse();
            response.setSuccess(true);
            response.setMessage("Paiement initié. Validez sur votre téléphone.");
            response.setTransactionId(transaction.getId());
            response.setPayToken(transaction.getPayToken());
            if (!request.isDirectPayment()) response.setPayUrl(transaction.getPayUrl());

            return response;
        } catch (Exception e) {
            log.error("Erreur lors de l'appel Aangaraa: {}", e.getMessage());
            throw new RuntimeException("Erreur d'initialisation : " + e.getMessage());
        }
    }

    /**
     * Étape 2 : Traitement du Webhook (Aangaraa -> Backend)
     * Reçoit la confirmation instantanée
     */
    @Transactional
    public void processWebhook(Map<String, Object> payload) {
        String payToken = (String) payload.get("payToken");
        String statusFromApi = String.valueOf(payload.getOrDefault("status", "PENDING"));

        log.info("🔔 WEBHOOK RECU - Token: {}, Status: {}", payToken, statusFromApi);

        transactionRepository.findByPayToken(payToken).ifPresent(t -> {
            if ("PENDING".equals(t.getStatut())) {
                updateTransactionStatus(t, statusFromApi);
            }
        });
    }

    /**
     * Étape 3 : Scheduler (Vérification automatique)
     * Tourne toutes les 60 secondes pour les transactions "oubliées"
     */
    @Scheduled(fixedRate = 60000) 
    @Transactional
    public void checkPendingTransactions() {
        List<Transaction> pending = transactionRepository.findByStatut("PENDING");
        if (pending.isEmpty()) return;

        log.info("📅 Scheduler : Vérification de {} transactions PENDING", pending.size());

        for (Transaction t : pending) {
            try {
                long ageSeconds = java.time.Duration.between(t.getDateCreation(), LocalDateTime.now()).toSeconds();

                // On attend 30 secondes minimum avant de vérifier pour laisser le temps au client
                if (ageSeconds < 30) continue;

                // Après 15 minutes, on considère la transaction comme échouée
                if (ageSeconds > 900) {
                    t.setStatut("FAILED");
                    transactionRepository.save(t);
                    continue;
                }

                Map<String, Object> checkBody = new HashMap<>();
                checkBody.put("payToken", t.getPayToken());
                checkBody.put("app_key", APP_KEY);

                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    URL_CHECK,
                    HttpMethod.POST,
                    new HttpEntity<>(checkBody),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
                );
                
                if (response.getBody() != null) {
                    String apiStatus = String.valueOf(response.getBody().getOrDefault("status", "PENDING"));
                    updateTransactionStatus(t, apiStatus);
                }
            } catch (Exception e) {
                log.error("Erreur Scheduler pour transaction {}: {}", t.getId(), e.getMessage());
            }
        }
    }

    /**
     * Méthode commune pour mettre à jour le statut et le solde
     */
    private void updateTransactionStatus(Transaction t, String apiStatus) {
        String status = apiStatus.toUpperCase();
        
        if (status.contains("SUCCESS")) {
            t.setStatut("SUCCESS");
            updateVendeurSoldeOnSuccess(t);
            if (t.getQrCode() != null) {
                t.getQrCode().setEstUtilise(true);
                qrCodeRepository.save(t.getQrCode());
            }
            log.info("✅ Transaction {} mise à SUCCESS", t.getId());
        } else if (status.contains("FAIL") || status.contains("CANCEL")) {
            t.setStatut("FAILED");
            log.info("❌ Transaction {} mise à FAILED", t.getId());
        }
        transactionRepository.save(t);
    }

    private void calculateCommissionAndNetAmount(Transaction transaction) {
        ConfigurationFrais config = configurationFraisRepository.findById(1L).orElse(new ConfigurationFrais());
        BigDecimal commission = transaction.getMontant().multiply(config.getCommissionRate()).setScale(2, RoundingMode.HALF_UP);
        transaction.setCommissionAppliquee(commission);
        transaction.setMontantNet(transaction.getMontant().subtract(commission));
    }

    private void updateVendeurSoldeOnSuccess(Transaction transaction) {
        try {
            Vendeur vendeur = transaction.getQrCode().getVendeur();
            if (vendeur != null) {
                vendeurService.augmenterSolde(vendeur.getId(), transaction.getMontantNet());
            }
        } catch (Exception e) {
            log.error("Erreur mise à jour solde vendeur: {}", e.getMessage());
        }
    }

    private void validateRequest(InitiatePaymentRequest request) {
        if (request.getQrCodeId() == null) throw new IllegalArgumentException("ID QR Code manquant.");
        if (request.getTelephoneClient() == null) throw new IllegalArgumentException("Téléphone manquant.");
        if (request.getMontant() == null || request.getMontant().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Montant invalide.");
    }

    private Map<String, Object> prepareAangaraaPayload(InitiatePaymentRequest request, QRCode qrCode, Transaction transaction) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", request.getMontant().toPlainString());
        payload.put("description", "Paiement QR Code " + qrCode.getId());
        payload.put("app_key", APP_KEY);
        payload.put("transaction_id", transaction.getId().toString());
        
        // Utiliser la méthode getWebhookUrl() qui lit la variable d'environnement
        String notifyUrl = getWebhookUrl();
        payload.put("notify_url", notifyUrl);
        log.info("📤 URL de notification envoyée à AangaraaPay: {}", notifyUrl);
        
        
        // Return URL - Page de succès après paiement
        String returnUrl = "https://backend-qr-code-u2kx.onrender.com/api/payment/success";
        payload.put("return_url", returnUrl);
        log.info("📤 URL de retour envoyée à AangaraaPay: {}", returnUrl);


        if (request.isDirectPayment()) {
            String phone = request.getTelephoneClient().trim().replaceAll("[^0-9]", "");
            if (!phone.startsWith("237")) phone = "237" + phone;
            payload.put("phone_number", phone);
            payload.put("operator", request.getOperator());
            payload.put("devise_id", "XAF");
        }
        return payload;
    }
}

