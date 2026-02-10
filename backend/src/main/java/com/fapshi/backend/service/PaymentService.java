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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    private static final String APP_KEY = "NRYT-9742-EHQY-QB4B";

    private static final String URL_DIRECT = "https://api-production.aangaraa-pay.com/api/v1/no_redirect/payment";
    private static final String URL_REDIRECT = "https://api-production.aangaraa-pay.com/api/v1/redirect/payment";

    public PaymentInitResponse initiatePayment(InitiatePaymentRequest request) {
        // 1. Validations de base du DTO
        validateRequest(request);

        // 2. Récupération du QR
        QRCode qrCode = qrCodeRepository.findById(request.getQrCodeId())
                .orElseThrow(() -> new RuntimeException("QR Code ID " + request.getQrCodeId() + " non trouvé"));

        if (qrCode.isEstUtilise()) {
            throw new RuntimeException("Ce QR Code a déjà été payé.");
        }

        if (qrCode.getDateExpiration().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Ce QR Code est expiré.");
        }

        // 3. Comparaison ROBUSTE des montants (BigDecimal safe)
        // stripTrailingZeros() permet de comparer 30.00 et 30.0 sans erreur
        BigDecimal montantBase = qrCode.getMontant().stripTrailingZeros();
        BigDecimal montantRecu = request.getMontant().stripTrailingZeros();

        log.info("Vérification montant - Base: {} | Reçu: {}", montantBase, montantRecu);

        if (montantBase.compareTo(montantRecu) != 0) {
            throw new RuntimeException(String.format(
                "Le montant saisi (%s XAF) ne correspond pas au prix du produit (%s XAF)", 
                montantRecu.toPlainString(), 
                montantBase.toPlainString()
            ));
        }

        // 4. Créer transaction PENDING
        Transaction transaction = new Transaction();
        transaction.setQrCode(qrCode);
        transaction.setTelephoneClient(request.getTelephoneClient());
        transaction.setMontant(request.getMontant());
        transaction.setOperator(request.getOperator());
        transaction.setStatut("PENDING");
        
        // 🔶 CALCULER LA COMMISSION ET LE MONTANT NET
        calculateCommissionAndNetAmount(transaction);
        
        transaction = transactionRepository.save(transaction);

        // 5. Préparer l'appel API
        String url = request.isDirectPayment() ? URL_DIRECT : URL_REDIRECT;
        Map<String, Object> payload = prepareAangaraaPayload(request, qrCode, transaction);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            log.info("Appel Aangaraa URL: {} | Payload: {}", url, payload);
            AangaraaPaymentResponse apiResponse = restTemplate.postForObject(url, entity, AangaraaPaymentResponse.class);

            if (apiResponse == null || apiResponse.getData() == null) {
                throw new RuntimeException("Réponse vide de l'API de paiement");
            }

            // 6. Mise à jour transaction avec infos Aangaraa
            transaction.setPayToken(apiResponse.getData().getPayToken());
            transaction.setPayUrl(apiResponse.getData().getPayment_url());
            transaction.setReferenceOperateur(apiResponse.getData().getTransaction_id());
            transactionRepository.save(transaction);

            // 7. Construction de la réponse finale
            PaymentInitResponse response = new PaymentInitResponse();
            response.setSuccess(true);
            response.setMessage("Paiement initié. Veuillez valider sur votre téléphone.");
            response.setTransactionId(transaction.getId());
            response.setPayToken(transaction.getPayToken());
            if (!request.isDirectPayment()) {
                response.setPayUrl(transaction.getPayUrl());
            }

            return response;

        }catch (org.springframework.web.client.HttpClientErrorException e) {
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
        // 4. Préparer le payload exact selon la doc Aangaraa
        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", request.getMontant().toPlainString());  // ← CORRECTION ICI
        payload.put("description", "Paiement via QR " + qrCode.getId());
        payload.put("app_key", APP_KEY);
        payload.put("transaction_id", transaction.getId().toString());
        payload.put("notify_url", "https://hector-noncrenated-nondyspeptically.ngrok-free.dev/api/webhook/aangaraa");

        if (request.isDirectPayment()) {
            payload.put("phone_number", request.getTelephoneClient());
            payload.put("operator", request.getOperator());
            payload.put("devise_id", "XAF");
        } else {
            payload.put("return_url", "https://example.com/payment-success");
        }
        return payload;
    }

    @Scheduled(fixedRate = 300000) // toutes les 5 minutes
    public void checkPendingTransactions() {
        List<Transaction> pending = transactionRepository.findByStatut("PENDING");
        for (Transaction t : pending) {
            if (t.getDateCreation().isBefore(LocalDateTime.now().minusMinutes(15))) {
                // Vérifier statut réel via Aangaraa
                try {
                    Map<String, Object> checkBody = new HashMap<>();
                    checkBody.put("payToken", t.getPayToken());
                    checkBody.put("app_key", APP_KEY);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(checkBody, headers);

                    Map<String, Object> response = restTemplate.postForObject(
                        "https://api-production.aangaraa-pay.com/api/v1/aangaraa_check_status",
                        entity,
                        Map.class
                    );

                    String status = (String) response.get("status");
                    if ("SUCCESSFUL".equals(status)) {
                        t.setStatut("SUCCESS");
                        // 🔶 Mettre à jour le solde du vendeur
                        updateVendeurSoldeOnSuccess(t);
                        // Marquer QR comme utilisé
                        QRCode qr = t.getQrCode();
                        qr.setEstUtilise(true);
                        qrCodeRepository.save(qr);
                    } else if ("FAILED".equals(status) || "CANCELLED".equals(status)) {
                        t.setStatut("FAILED");
                    }
                    transactionRepository.save(t);
                    log.info("Transaction {} mise à jour à {}", t.getId(), status);
                } catch (Exception e) {
                    log.error("Erreur check pending transaction {}: {}", t.getId(), e.getMessage());
                }
            }
        }
    }

    /**
     * Calcule la commission et le montant net pour une transaction
     * Commission actuelle : 0% (configurable via ConfigurationFrais.commissionRate)
     * 
     * @param transaction Transaction à mettre à jour
     */
    private void calculateCommissionAndNetAmount(Transaction transaction) {
        // Récupérer la configuration des frais
        ConfigurationFrais config = configurationFraisRepository.findById(1L)
                .orElse(new ConfigurationFrais()); // Défaut si pas trouvé
        
        BigDecimal commissionRate = config.getCommissionRate(); // Par défaut 0.00
        BigDecimal montantBrut = transaction.getMontant();
        
        // Commission = montant brut × taux
        BigDecimal commission = montantBrut
                .multiply(commissionRate)
                .setScale(2, RoundingMode.HALF_UP);
        
        // Montant net = montant brut - commission
        BigDecimal montantNet = montantBrut.subtract(commission);
        
        transaction.setCommissionAppliquee(commission);
        transaction.setMontantNet(montantNet);
        
        log.info("Commission calculée - Brut: {} XAF, Commission: {} XAF ({%}), Net: {} XAF",
                montantBrut, commission, commissionRate.multiply(new BigDecimal("100")), montantNet);
    }

    /**
     * Met à jour le solde du vendeur lors d'une transaction SUCCESS
     * Ajoute le montant net au solde virtuel du vendeur
     * 
     * @param transaction Transaction qui vient de devenir SUCCESS
     */
    private void updateVendeurSoldeOnSuccess(Transaction transaction) {
        try {
            // Récupérer le vendeur propriétaire du QR code
            Vendeur vendeur = transaction.getQrCode().getVendeur();
            if (vendeur == null) {
                log.error("Vendeur non trouvé pour QR Code ID: {}", transaction.getQrCode().getId());
                return;
            }
            
            // Ajouter le montant net au solde
            vendeurService.augmenterSolde(vendeur.getId(), transaction.getMontantNet());
            
            log.info("Solde vendeur {} augmenté de {} XAF (transaction SUCCESS ID: {})",
                    vendeur.getId(), transaction.getMontantNet(), transaction.getId());
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du solde pour transaction {}: {}", 
                    transaction.getId(), e.getMessage());
        }
    }}