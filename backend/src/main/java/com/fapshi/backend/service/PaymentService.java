package com.fapshi.backend.service;

import java.math.BigDecimal;
import com.fapshi.backend.dto.external.AangaraaPaymentResponse;
import com.fapshi.backend.dto.request.InitiatePaymentRequest;
import com.fapshi.backend.dto.response.PaymentInitResponse;
import com.fapshi.backend.entity.QRCode;
import com.fapshi.backend.entity.Transaction;
import com.fapshi.backend.repository.QRCodeRepository;
import com.fapshi.backend.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private QRCodeRepository qrCodeRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RestTemplate restTemplate;

    private static final String APP_KEY = "VOTRE_APP_KEY_ICI"; // Remplace par ta vraie clé

    private static final String URL_DIRECT = "https://api-production.aangaraa-pay.com/api/v1/no_redirect/payment";
    private static final String URL_REDIRECT = "https://api-production.aangaraa-pay.com/api/v1/redirect/payment";

    public PaymentInitResponse initiatePayment(InitiatePaymentRequest request) {
        // Validation renforcée
        if (request.getQrCodeId() == null) {
            throw new IllegalArgumentException("ID du QR code requis");
        }
        if (request.getTelephoneClient() == null || request.getTelephoneClient().trim().isEmpty()) {
            throw new IllegalArgumentException("Numéro de téléphone requis pour le paiement");
        }
        if (request.getOperator() == null || 
            (!"Orange_Cameroon".equals(request.getOperator()) && !"MTN_Cameroon".equals(request.getOperator()))) {
            throw new IllegalArgumentException("Opérateur invalide : Orange_Cameroon ou MTN_Cameroon uniquement");
        }
        if (request.getMontant() == null || request.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Montant invalide");
        }

        // 1. Valider le QR
        QRCode qrCode = qrCodeRepository.findById(request.getQrCodeId())
                .orElseThrow(() -> new RuntimeException("QR Code non trouvé"));

        if (qrCode.isEstUtilise()) {
            throw new RuntimeException("QR Code déjà utilisé");
        }

        if (qrCode.getDateExpiration().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("QR Code expiré");
        }

        if (!qrCode.getMontant().equals(request.getMontant())) {
            throw new RuntimeException("Montant ne correspond pas au QR");
        }

        // 2. Créer transaction PENDING
        Transaction transaction = new Transaction();
        transaction.setQrCode(qrCode);
        transaction.setTelephoneClient(request.getTelephoneClient());
        transaction.setMontant(request.getMontant());
        transaction.setOperator(request.getOperator());
        transaction.setStatut("PENDING");
        transaction = transactionRepository.save(transaction);

        // 3. Choisir l'URL selon le mode
        String url = request.isDirectPayment() ? URL_DIRECT : URL_REDIRECT;

        // 4. Préparer le payload exact selon la doc Aangaraa
        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", request.getMontant().toString());
        payload.put("description", "Paiement via QR " + qrCode.getId());
        payload.put("app_key", APP_KEY);
        payload.put("transaction_id", transaction.getId().toString());
        payload.put("notify_url", "https://ton-domaine.com/api/webhook/aangaraa");

        if (request.isDirectPayment()) {
            payload.put("phone_number", request.getTelephoneClient());
            payload.put("operator", request.getOperator());
            payload.put("devise_id", "XAF");
        } else {
            payload.put("return_url", "https://ton-app.com/payment-success");
        }

        // 5. Appel API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        log.info("Envoi requête à Aangaraa : {} - Payload: {}", url, payload);

        try {
            AangaraaPaymentResponse apiResponse = restTemplate.postForObject(url, entity, AangaraaPaymentResponse.class);

            // 6. Parser la réponse réelle
            transaction.setPayToken(apiResponse.getData().getPayToken());
            transaction.setPayUrl(apiResponse.getData().getPayment_url());
            transaction.setReferenceOperateur(apiResponse.getData().getTransaction_id());
            transaction.setStatut("PENDING");
            transactionRepository.save(transaction);

            log.info("Paiement initié avec succès - payToken: {}, payUrl: {}", 
                     transaction.getPayToken(), transaction.getPayUrl());

            // 7. Réponse au client
            PaymentInitResponse response = new PaymentInitResponse();
            response.setSuccess(true);
            response.setMessage("Paiement initié avec succès");
            response.setTransactionId(transaction.getId());
            response.setPayToken(transaction.getPayToken());

            if (!request.isDirectPayment()) {
                response.setPayUrl(transaction.getPayUrl());
            }

            return response;

        } catch (Exception e) {
            log.error("Erreur lors de l'appel Aangaraa : {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'initialisation du paiement : " + e.getMessage());
        }
    }
}