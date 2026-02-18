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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @Autowired
    private QRCodeRepository qrCodeRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ConfigurationFraisRepository configurationFraisRepository;

    @Autowired
    private VendeurService vendeurService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${app.aangaraa.webhook-url:https://backend-qr-code-u2kx.onrender.com/api/webhook/aangaraa}")
    private String webhookUrl;

    private static final String APP_KEY = "NRYT-9742-EHQY-QB4B";

    private static final String URL_DIRECT   = "https://api-production.aangaraa-pay.com/api/v1/no_redirect/payment";
    private static final String URL_REDIRECT = "https://api-production.aangaraa-pay.com/api/v1/redirect/payment";

    public PaymentInitResponse initiatePayment(InitiatePaymentRequest request) {
        validateRequest(request);

        QRCode qrCode = qrCodeRepository.findById(request.getQrCodeId())
                .orElseThrow(() -> new RuntimeException("QR Code ID " + request.getQrCodeId() + " non trouvé"));

        if (qrCode.isEstUtilise()) {
            throw new RuntimeException("Ce QR Code a déjà été payé.");
        }
        if (qrCode.getDateExpiration().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Ce QR Code est expiré.");
        }

        BigDecimal montantBase = qrCode.getMontant().stripTrailingZeros();
        BigDecimal montantRecu = request.getMontant().stripTrailingZeros();

        if (montantBase.compareTo(montantRecu) != 0) {
            throw new RuntimeException(String.format(
                "Le montant saisi (%s XAF) ne correspond pas au prix du produit (%s XAF)",
                montantRecu.toPlainString(), montantBase.toPlainString()));
        }

        Transaction transaction = new Transaction();
        transaction.setQrCode(qrCode);
        transaction.setTelephoneClient(request.getTelephoneClient());
        transaction.setMontant(request.getMontant());
        transaction.setOperator(request.getOperator());
        transaction.setStatut("PENDING");

        calculateCommissionAndNetAmount(transaction);
        transaction = transactionRepository.save(transaction);

        String url = request.isDirectPayment() ? URL_DIRECT : URL_REDIRECT;
        Map<String, Object> payload = prepareAangaraaPayload(request, qrCode, transaction);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            log.info("Appel Aangaraa URL: {} | Payload: {}", url, payload);

            AangaraaPaymentResponse apiResponse = restTemplate.postForObject(url, entity, AangaraaPaymentResponse.class);

            if (apiResponse == null || apiResponse.getData() == null) {
                throw new RuntimeException("Réponse vide de l'API Aangaraa");
            }

            AangaraaPaymentResponse.Data data = apiResponse.getData();

            log.info("Réponse Aangaraa reçue → payToken: {}, status: {}, transaction_id: {}",
                    data.getPayToken(), data.getStatus(), data.getTransaction_id());

            transaction.setPayToken(data.getPayToken());
            transaction.setPayUrl(data.getPayment_url());
            transaction.setReferenceOperateur(data.getTransaction_id());
            transactionRepository.save(transaction);

            PaymentInitResponse response = new PaymentInitResponse();
            response.setSuccess(true);
            response.setMessage("Paiement initié. Veuillez valider sur votre téléphone.");
            response.setTransactionId(transaction.getId());
            response.setPayToken(transaction.getPayToken());

            if (!request.isDirectPayment()) {
                response.setPayUrl(transaction.getPayUrl());
            }

            return response;

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("Erreur Aangaraa - Status: {}, Body: {}", e.getStatusCode(), errorBody);
            throw new RuntimeException("Erreur paiement Aangaraa : " + e.getStatusCode() + " - " + errorBody);
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'appel Aangaraa", e);
            throw new RuntimeException("Erreur lors de l'initialisation du paiement : " + e.getMessage());
        }
    }

    private void validateRequest(InitiatePaymentRequest request) {
        if (request.getQrCodeId() == null) throw new IllegalArgumentException("ID QR Code manquant.");
        if (request.getTelephoneClient() == null || request.getTelephoneClient().length() < 9)
            throw new IllegalArgumentException("Numéro de téléphone invalide.");
        if (request.getMontant() == null || request.getMontant().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Montant invalide.");
    }

    private Map<String, Object> prepareAangaraaPayload(InitiatePaymentRequest request, QRCode qrCode, Transaction transaction) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", request.getMontant().toPlainString());
        payload.put("description", "Paiement via QR " + qrCode.getId());
        payload.put("app_key", APP_KEY);
        payload.put("transaction_id", transaction.getId().toString());
        payload.put("notify_url", webhookUrl);

        log.info("Notify URL envoyée à Aangaraa : {}", webhookUrl);

        if (request.isDirectPayment()) {
            String phone = request.getTelephoneClient().trim().replaceAll("[^0-9]", "");
            if (phone.startsWith("0")) phone = phone.substring(1);
            if (!phone.startsWith("237")) phone = "237" + phone;

            payload.put("phone_number", phone);
            payload.put("operator", request.getOperator());
            payload.put("devise_id", "XAF");
            payload.put("return_url", "https://example.com/payment-success");
        } else {
            payload.put("return_url", "https://example.com/payment-success");
        }
        return payload;
    }

    @Scheduled(fixedRate = 900000) // Toutes les 15 minutes
    @Transactional
    public void checkPendingTransactions() {
        log.info("📅 Scheduler lancé : Vérification des transactions PENDING");

        List<Transaction> pending = transactionRepository.findByStatut("PENDING");
        log.info("Nombre de transactions PENDING : {}", pending.size());

        for (Transaction t : pending) {
            try {
                long ageMinutes = java.time.Duration.between(t.getDateCreation(), LocalDateTime.now()).toMinutes();

                log.info("Vérification ID: {}, payToken: {}, âge: {} min", t.getId(), t.getPayToken(), ageMinutes);

                if (t.getPayToken() == null || t.getPayToken().isBlank()) {
                    log.warn("Transaction {} ignorée car payToken NULL", t.getId());
                    t.setStatut("FAILED");
                    transactionRepository.save(t);
                    continue;
                }

                if (ageMinutes > 15) {
                    log.warn("Transaction {} expirée automatiquement (>15 min)", t.getId());
                    t.setStatut("FAILED");
                    transactionRepository.save(t);
                    continue;
                }

                if (ageMinutes < 5) {
                    log.info("Transaction {} encore récente (<5 min), on attend...", t.getId());
                    continue;
                }

                Map<String, Object> checkBody = new HashMap<>();
                checkBody.put("payToken", t.getPayToken());
                checkBody.put("app_key", APP_KEY);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(checkBody, headers);

                ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                        "https://api-production.aangaraa-pay.com/api/v1/aangaraa_check_status",
                        HttpMethod.POST,
                        entity,
                        new ParameterizedTypeReference<Map<String, Object>>() {}
                );

                Map<String, Object> response = responseEntity.getBody();
                if (response == null) {
                    log.error("Réponse API NULL pour transaction {}", t.getId());
                    continue;
                }

                log.info("Réponse API : {}", response);

                String status = String.valueOf(response.getOrDefault("status", "UNKNOWN")).toUpperCase();

                switch (status) {
                    case "SUCCESSFUL":
                        t.setStatut("SUCCESS");
                        updateVendeurSoldeOnSuccess(t);
                        QRCode qr = t.getQrCode();
                        if (qr != null) {
                            qr.setEstUtilise(true);
                            qrCodeRepository.save(qr);
                        }
                        log.info("Transaction {} validée SUCCESS", t.getId());
                        break;

                    case "FAILED":
                    case "CANCELLED":
                        t.setStatut("FAILED");
                        log.info("Transaction {} échouée", t.getId());
                        break;

                    case "PENDING":
                        log.info("Transaction {} toujours PENDING", t.getId());
                        break;

                    default:
                        log.warn("Transaction {} statut inconnu: {}", t.getId(), status);
                        t.setStatut("UNKNOWN");
                        break;
                }

                transactionRepository.save(t);
                log.info("Mise à jour : {} → {}", t.getId(), t.getStatut());

            } catch (Exception e) {
                log.error("Erreur vérification ID {} : {}", t.getId(), e.getMessage(), e);
            }
        }

        log.info("📅 Scheduler terminé");
    }

    private void calculateCommissionAndNetAmount(Transaction transaction) {
        ConfigurationFrais config = configurationFraisRepository.findById(1L)
                .orElse(new ConfigurationFrais());

        BigDecimal commissionRate = config.getCommissionRate();
        BigDecimal montantBrut = transaction.getMontant();

        BigDecimal commission = montantBrut
                .multiply(commissionRate)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal montantNet = montantBrut.subtract(commission);

        transaction.setCommissionAppliquee(commission);
        transaction.setMontantNet(montantNet);

        log.info("Commission calculée - Brut: {} XAF, Commission: {} XAF ({}%), Net: {} XAF",
                montantBrut, commission, commissionRate.multiply(new BigDecimal("100")), montantNet);
    }

    private void updateVendeurSoldeOnSuccess(Transaction transaction) {
        try {
            Vendeur vendeur = transaction.getQrCode().getVendeur();
            if (vendeur == null) {
                log.error("Vendeur non trouvé pour QR Code ID: {}", transaction.getQrCode().getId());
                return;
            }
            vendeurService.augmenterSolde(vendeur.getId(), transaction.getMontantNet());
            log.info("Solde vendeur {} augmenté de {} XAF (transaction SUCCESS ID: {})",
                    vendeur.getId(), transaction.getMontantNet(), transaction.getId());
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du solde pour transaction {}: {}", 
                    transaction.getId(), e.getMessage(), e);
        }
    }
}