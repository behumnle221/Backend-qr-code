/*
 * Copyright (c) 2024. Fapshi Inc.
 */

package com.fapshi.backend.service;

import com.fapshi.backend.dto.external.AangaraaPaymentResponse;
import com.fapshi.backend.dto.request.InitiatePaymentRequest;
import com.fapshi.backend.dto.request.RechargementRequest;
import com.fapshi.backend.dto.request.VirtualPaymentRequest;
import com.fapshi.backend.dto.response.PaymentInitResponse;
import com.fapshi.backend.entity.AangaraaPayRequest;
import com.fapshi.backend.entity.AangaraaPayResponse;
import com.fapshi.backend.entity.Client;
import com.fapshi.backend.entity.ConfigurationFrais;
import com.fapshi.backend.entity.QRCode;
import com.fapshi.backend.entity.Transaction;
import com.fapshi.backend.entity.Vendeur;
import com.fapshi.backend.enums.StatutTransaction;
import com.fapshi.backend.enums.TypeRequest;
import com.fapshi.backend.enums.TypeTransaction;
import com.fapshi.backend.repository.AangaraaPayRequestRepository;
import com.fapshi.backend.repository.AangaraaPayResponseRepository;
import com.fapshi.backend.repository.ConfigurationFraisRepository;
import com.fapshi.backend.repository.QRCodeRepository;
import com.fapshi.backend.repository.TransactionRepository;
import com.fapshi.backend.repository.RetraitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    @Autowired private RetraitRepository auteurRepository;
    @Autowired private VendeurService vendeurService;
    @Autowired private ClientService clientService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private RestTemplate restTemplate;
    
    @Autowired
    private AangaraaPayRequestRepository aangaraaPayRequestRepository;
    
    @Autowired
    private AangaraaPayResponseRepository aangaraaPayResponseRepository;

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
     * Initie un rechargement du compte virtuel du client via Aangaraa
     * Le client fournit son montant et son opérateur, puis valide sur son téléphone
     */
    @Transactional
    public PaymentInitResponse initierRechargement(Long clientId, RechargementRequest request) {
        // Validation
        if (request.getMontant() == null || request.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Le montant doit être positif");
        }
        if (request.getMontant().compareTo(new BigDecimal("10")) < 0) {
            throw new RuntimeException("Le montant minimum est de 10 XAF");
        }
        if (request.getOperator() == null || request.getOperator().isBlank()) {
            throw new RuntimeException("L'opérateur est requis");
        }
        
        // Récupérer le client
        Client client = clientService.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client introuvable"));
        
        // Créer une transaction de type RECHARGEMENT
        Transaction transaction = new Transaction();
        transaction.setClient(client);
        transaction.setMontant(request.getMontant());
        transaction.setOperator(request.getOperator());
        transaction.setTransactionType(TypeTransaction.RECHARGEMENT);
        transaction.setStatut("PENDING");
        transaction.setTelephoneClient(client.getTelephone());
        transaction.setDateCreation(LocalDateTime.now());
        
        // Générer le transactionId
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 10000);
        transaction.setTransactionId("RECH_" + timestamp + "_" + random);
        
        transaction = transactionRepository.save(transaction);
        log.info("✅ Transaction de rechargement créée: ID={}, montant={}", transaction.getId(), request.getMontant());
        
        // Préparer le payload pour Aangaraa
        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", request.getMontant().toString());
        payload.put("description", "Rechargement compte virtuel");
        payload.put("app_key", APP_KEY);
        payload.put("transaction_id", transaction.getId().toString());
        
        String notifyUrl = getWebhookUrl();
        payload.put("notify_url", notifyUrl);
        
        String returnUrl = "https://backend-qr-code-u2kx.onrender.com/api/payments/success";
        
        payload.put("return_url", returnUrl);
        
        // Ajouter les infos téléphone 
        // Utiliser le téléphone de la requête si fourni, sinon celui du client

        String phone;
        if (request.getTelephone() != null && !request.getTelephone().isBlank()) {
            phone = request.getTelephone().trim().replaceAll("[^0-9]", "");
        } else {
            phone = client.getTelephone().trim().replaceAll("[^0-9]", "");
        }
        if (!phone.startsWith("237")) phone = "237" + phone;
        payload.put("phone_number", phone);
        payload.put("operator", request.getOperator());
        payload.put("devise_id", "XAF");
        
        // Appel à Aangaraa
        String url = request.isDirectPayment() ? URL_DIRECT : URL_REDIRECT;
        
        // DEBUG: Log complet du payload
        log.info("🔍 DEBUG PAYLOAD AANGARAA - URL: {}", url);
        log.info("🔍 DEBUG PAYLOAD: amount={}, phone={}, operator={}, devise_id={}, transaction_id={}, notify_url={}, return_url={}", 
            payload.get("amount"), payload.get("phone_number"), payload.get("operator"), 
            payload.get("devise_id"), payload.get("transaction_id"), payload.get("notify_url"), payload.get("return_url"));
        
        try {
            log.info("📤 Appel Aangaraa pour rechargement: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            ResponseEntity<AangaraaPaymentResponse> responseEntity = restTemplate.exchange(
                url, HttpMethod.POST, entity, AangaraaPaymentResponse.class);
            
            AangaraaPaymentResponse apiResponse = responseEntity.getBody();
            
            // DEBUG: Log réponse complète
            log.info("🔍 DEBUG REPONSE AANGARAA: statusCode={}, message={}, data={}", 
                apiResponse.getStatusCode(), apiResponse.getMessage(), apiResponse.getData());
            
            if (apiResponse == null) {
                throw new RuntimeException("Réponse vide de l'API Aangaraa");
            }
            
            Integer statusCode = apiResponse.getStatusCode();
            if (statusCode == null || (statusCode != 200 && statusCode != 201)) {
                throw new RuntimeException("Erreur Aangaraa: " + apiResponse.getMessage());
            }
            
            // Sauvegarder la requête et réponse Aangaraa dans une transaction séparée
            // (ne bloque pas le rechargement si erreur)
            try {
                saveAangaraaDataAsync(request.getMontant(), 
                    request.getTelephone() != null ? request.getTelephone() : client.getTelephone(),
                    request.getOperator(), statusCode, apiResponse);
            } catch (Exception e) {
                log.warn("⚠️  Erreur sauvegarde Aangaraa (non-bloquant): {}", e.getMessage());
            }
            
            if (apiResponse.getData() == null) {
                throw new RuntimeException("Données vides dans la réponse Aangaraa");
            }
            
            // Sauvegarder le payToken
            AangaraaPaymentResponse.Data data = apiResponse.getData();
            transaction.setPayToken(data.getPayToken());
            transaction.setPayUrl(data.getPayment_url());
            transaction.setReferenceOperateur(data.getTransaction_id());
            transactionRepository.save(transaction);
            
            log.info("✅ Rechargement initié, payToken: {}", data.getPayToken());
            
            PaymentInitResponse response = new PaymentInitResponse();
            response.setSuccess(true);
            response.setMessage("Rechargement initié. Validez sur votre téléphone.");
            response.setTransactionId(transaction.getId());
            response.setPayToken(transaction.getPayToken());
            if (!request.isDirectPayment()) response.setPayUrl(transaction.getPayUrl());
            
            return response;
            
        } catch (Exception e) {
            log.error("❌ Erreur lors du rechargement: {}", e.getMessage());
            throw new RuntimeException("Erreur lors du rechargement: " + e.getMessage());
        }
    }

    /**
     * Vérifie le statut du paiement directement auprès d'Aangaraa
     */
    public Map<String, Object> checkPaymentStatus(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction non trouvée"));
        
        if (transaction.getPayToken() == null) {
            if (transaction.getTransactionType() != null &&
                (transaction.getTransactionType() == TypeTransaction.PAYMENT_MARCHAND ||
                 transaction.getTransactionType() == TypeTransaction.TRANSFERT_VIRTUEL)) {
                return Map.of(
                    "status", transaction.getStatut(),
                    "message", "Transaction interne",
                    "transactionType", transaction.getTransactionType().name()
                );
            }
            throw new RuntimeException("Aucun payToken pour cette transaction");
        }
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("app_key", APP_KEY);
        payload.put("payToken", transaction.getPayToken());
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                URL_CHECK, HttpMethod.POST, entity, Map.class);
            
            Map<String, Object> responseBody = response.getBody();
            log.info("📊 Réponse status check: {}", responseBody);
            
            if (responseBody != null && responseBody.get("status") != null) {
                String status = responseBody.get("status").toString();
                transaction.setStatut(status);
                
                if ("SUCCESSFUL".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
                    handlePaymentSuccess(transaction);
                }
                
                transactionRepository.save(transaction);
            }
            
            return responseBody;
        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification du statut: {}", e.getMessage());
            throw new RuntimeException("Erreur vérification statut: " + e.getMessage());
        }
    }

    /**
     * Méthode utilitaire pour gérer le succès du paiement (extraite pour être réutilisable)
     */
    private void handlePaymentSuccess(Transaction transaction) {
        try {
            // Marquer le QR code comme utilisé
            QRCode qrCode = transaction.getQrCode();
            if (qrCode != null) {
                qrCode.setEstUtilise(true);
                qrCodeRepository.save(qrCode);
                log.info("✅ QR Code {} marqué comme utilisé", qrCode.getId());
            }
            
            // Créditer le vendeur
            Vendeur vendeur = qrCode != null ? qrCode.getVendeur() : null;
            if (vendeur != null) {
                BigDecimal montantNet = transaction.getMontantNet() != null ? 
                    transaction.getMontantNet() : transaction.getMontant();
                auteurService.augmenterSolde(vendeur.getId(), montantNet);
                log.info("💰 Vendeur {} crédité de {} XAF", vendeur.getId(), montantNet);
            }
        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement du succès: {}", e.getMessage());
        }
    }

    /**
     * Étape 1 : Initialisation du paiement (Mobile -> Backend)
     */
    @Transactional
    public PaymentInitResponse initiatePayment(InitiatePaymentRequest request) {
        try {
            validateRequest(request);
            log.info("✅ Requête validée");
        } catch (Exception e) {
            log.error("❌ Erreur validation: {}", e.getMessage());
            throw new RuntimeException("Erreur validation: " + e.getMessage());
        }

        QRCode qrCode;
        try {
            qrCode = qrCodeRepository.findById(request.getQrCodeId())
                    .orElseThrow(() -> new RuntimeException("QR Code non trouvé"));
            log.info("✅ QR Code trouvé: {}", qrCode.getId());
        } catch (Exception e) {
            log.error("❌ Erreur QR Code: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }

        try {
            if (qrCode.isEstUtilise()) throw new RuntimeException("QR Code déjà payé.");
            if (qrCode.getDateExpiration().isBefore(LocalDateTime.now())) throw new RuntimeException("QR Code expiré.");
            log.info("✅ QR Code valide pour paiement");
        } catch (Exception e) {
            log.error("❌ Erreur validation QR: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }

        // Création de la transaction en base de données
        Transaction transaction = new Transaction();
        transaction.setQrCode(qrCode);
        transaction.setTelephoneClient(request.getTelephoneClient());
        transaction.setMontant(request.getMontant());
        transaction.setOperator(request.getOperator());
        transaction.setStatut("PENDING");
        transaction.setDateCreation(LocalDateTime.now());
        
        // Générer le transactionId au format TRANS_1769339875485 (TRANS_timestamp + random)
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 10000); // 4 chiffres aléatoires
        transaction.setTransactionId("TRANS_" + timestamp + "_" + random);

        TypeTransaction requestedType = determineTransactionType(request, qrCode);

        if (requestedType == TypeTransaction.PAYMENT_MARCHAND || requestedType == TypeTransaction.TRANSFERT_VIRTUEL) {
            if (qrCode.getVendeur() == null) {
                throw new RuntimeException("Le QR Code n'est associé à aucun commerçant.");
            }
            if (request.getTelephoneClient() == null || request.getTelephoneClient().isBlank()) {
                throw new RuntimeException("Le numéro du client est requis pour un paiement marchand interne.");
            }

            Client client = clientService.findByTelephone(request.getTelephoneClient())
                    .orElseThrow(() -> new RuntimeException("Client introuvable pour le numéro : " + request.getTelephoneClient()));

            calculateCommissionAndNetAmount(transaction);
            clientService.debiterSolde(client.getId(), transaction.getMontant());

            Vendeur vendeur = qrCode.getVendeur();
            BigDecimal montantNet = transaction.getMontantNet() != null ? transaction.getMontantNet() : transaction.getMontant();
            auteurService.augmenterSolde(vendeur.getId(), montantNet);

            qrCode.setEstUtilise(true);
            qrCodeRepository.save(qrCode);

            transaction.setClient(client);
            transaction.setTransactionType(TypeTransaction.TRANSFERT_VIRTUEL);
            transaction.setStatut("SUCCESS");
            transactionRepository.save(transaction);

            auditLogService.log(client.getId(), client.getTelephone(), "PAYMENT_MARCHAND", 
                    "Paiement QR interne vers vendeur " + vendeur.getId() + " montant " + transaction.getMontant(), transaction);

            PaymentInitResponse response = new PaymentInitResponse();
            response.setSuccess(true);
            response.setMessage("Paiement marchand interne effectué avec succès.");
            response.setTransactionId(transaction.getId());
            return response;
        }

        transaction.setTransactionType(requestedType);
        calculateCommissionAndNetAmount(transaction);
        transaction = transactionRepository.save(transaction);
        log.info("✅ Transaction créée: ID={}, transactionId={}", transaction.getId(), transaction.getTransactionId());

        // Préparation de l'appel vers Aangaraa
        String url = request.isDirectPayment() ? URL_DIRECT : URL_REDIRECT;
        Map<String, Object> payload = prepareAangaraaPayload(request, qrCode, transaction);
        log.info("✅ Payload préparé pour Aangaraa: {}", payload);

        try {
            log.info("Appel Aangaraa URL: {} | Transaction ID: {}", url, transaction.getId());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            ResponseEntity<AangaraaPaymentResponse> responseEntity = restTemplate.exchange(
                url, HttpMethod.POST, entity, AangaraaPaymentResponse.class);
            
            AangaraaPaymentResponse apiResponse = responseEntity.getBody();

            if (apiResponse == null) {
                throw new RuntimeException("Réponse vide de l'API Aangaraa");
            }

            log.info("📥 Réponse Aangaraa - StatusCode: {}, Message: {}", apiResponse.getStatusCode(), apiResponse.getMessage());

            if (apiResponse.getStatusCode() != 200 && apiResponse.getStatusCode() != 201) {
                throw new RuntimeException("Erreur Aangaraa: " + apiResponse.getMessage());
            }

            if (apiResponse.getData() == null) {
                throw new RuntimeException("Données vides dans la réponse Aangaraa");
            }

            AangaraaPaymentResponse.Data data = apiResponse.getData();
            transaction.setPayToken(data.getPayToken());
            transaction.setPayUrl(data.getPayment_url());
            transaction.setReferenceOperateur(data.getTransaction_id());
            transactionRepository.save(transaction);
            log.info("✅ Réponse Aangaraa reçue, token: {}", data.getPayToken());

            PaymentInitResponse response = new PaymentInitResponse();
            response.setSuccess(true);
            response.setMessage("Paiement initié. Validez sur votre téléphone.");
            response.setTransactionId(transaction.getId());
            response.setPayToken(transaction.getPayToken());
            if (!request.isDirectPayment()) response.setPayUrl(transaction.getPayUrl());

            return response;
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("❌ Erreur de connexion à Aangaraa (timeout ou connexion refusée): {}", e.getMessage());
            log.error("❌ Détails: ", e);
            throw new RuntimeException("Impossible de se connecter au service de paiement. Veuillez réessayer plus tard.");
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("❌ Erreur HTTP de l'API Aangaraa: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Erreur du service de paiement: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'appel Aangaraa: {} - Type: {}", e.getMessage(), e.getClass().getName());
            log.error("❌ Stack trace: ", e);
            throw new RuntimeException("Erreur d'initialisation du paiement: " + e.getMessage());
        }
    }

    /**
     * Paiement simplifié par solde virtuel (TRANSFERT_VIRTUEL)
     * Aucun appel à Aangaraa, débit immédiat du solde client
     */
    @Transactional
    public PaymentInitResponse initiateVirtualPayment(Long clientId, VirtualPaymentRequest request) {
        try {
            // Validation
            if (request.getQrCodeId() == null) {
                throw new RuntimeException("L'ID du QR Code est requis");
            }
            if (request.getMontant() == null || request.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Le montant doit être positif");
            }
            log.info("✅ Requête validation ok");

            // Récupérer le QR Code
            QRCode qrCode = qrCodeRepository.findById(request.getQrCodeId())
                    .orElseThrow(() -> new RuntimeException("QR Code non trouvé: " + request.getQrCodeId()));
            
            // Valider le QR Code
            if (qrCode.isEstUtilise()) {
                throw new RuntimeException("QR Code déjà payé");
            }
            if (qrCode.getDateExpiration().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("QR Code expiré");
            }
            if (qrCode.getVendeur() == null) {
                throw new RuntimeException("Le QR Code n'est associé à aucun vendeur");
            }
            log.info("✅ QR Code valide");

            // Récupérer le client
            Client client = clientService.findById(clientId)
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));
            
            // Vérifier que le client a assez de solde
            BigDecimal solde = clientService.getSoldeVirtuel(clientId);
            if (solde.compareTo(request.getMontant()) < 0) {
                throw new RuntimeException("Solde insuffisant. Solde: " + solde + " XAF, Montant demandé: " + request.getMontant() + " XAF");
            }
            log.info("✅ Solde suffisant: {} XAF", solde);

            // Créer la transaction
            Transaction transaction = new Transaction();
            transaction.setQrCode(qrCode);
            transaction.setClient(client);
            transaction.setTelephoneClient(client.getTelephone());
            transaction.setMontant(request.getMontant());
            transaction.setTransactionType(TypeTransaction.TRANSFERT_VIRTUEL);
            transaction.setStatut("PENDING");
            transaction.setDateCreation(LocalDateTime.now());
            
            // Générer l'ID de transaction
            long timestamp = System.currentTimeMillis();
            int random = (int) (Math.random() * 10000);
            transaction.setTransactionId("TRANS_" + timestamp + "_" + random);
            
            // Calculer les frais et montant net
            calculateCommissionAndNetAmount(transaction);
            
            transaction = transactionRepository.save(transaction);
            log.info("✅ Transaction créée: ID={}, transactionId={}, montant={}", transaction.getId(), transaction.getTransactionId(), request.getMontant());

            // DÉBIT du compte du client
            clientService.debiterSolde(clientId, request.getMontant());
            log.info("💳 Solde client débité de {} XAF", request.getMontant());

// CRÉDIT du compte du vendeur
             Vendeur vendeur = qrCode.getVendeur();
             BigDecimal montantNet = transaction.getMontantNet() != null ? transaction.getMontantNet() : request.getMontant();
             vendeurService.augmenterSolde(vendeur.getId(), montantNet);
            log.info("💰 Vendeur {} crédité de {} XAF", vendeur.getId(), montantNet);

            // Marquer le QR code comme utilisé
            qrCode.setEstUtilise(true);
            qrCodeRepository.save(qrCode);
            log.info("✅ QR Code {} marqué comme utilisé", qrCode.getId());

            // Marquer la transaction comme réussie
            transaction.setStatut("SUCCESS");
            transactionRepository.save(transaction);
            log.info("✅ Transaction {} marquée SUCCESS", transaction.getId());

            // Log d'audit
            auditLogService.log(client.getId(), client.getTelephone(), "PAYMENT_VIRTUEL", 
                    "Paiement par solde virtuel vers vendeur " + vendeur.getId() + " montant " + request.getMontant(), transaction);

            // Réponse
            PaymentInitResponse response = new PaymentInitResponse();
            response.setSuccess(true);
            response.setMessage("Paiement par solde virtuel effectué avec succès");
            response.setTransactionId(transaction.getId());
            return response;
            
        } catch (Exception e) {
            log.error("❌ Erreur paiement virtuel: {}", e.getMessage());
            throw new RuntimeException("Erreur paiement virtuel: " + e.getMessage());
        }
    }

    /**
     * Sauvegarde les données Aangaraa dans une transaction indépendante
     * N'affecte pas la transaction principale si erreur
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    private void saveAangaraaDataAsync(BigDecimal montant, String telephone, String operator, 
                                       Integer statusCode, AangaraaPaymentResponse apiResponse) {
        try {
            AangaraaPayRequest req = new AangaraaPayRequest();
            req.setAmount(montant);
            req.setPhoneNumber(telephone);
            req.setOperator(operator);
            req.setTypeRequest(TypeRequest.RECHARGEMENT);
            req.setDescription("Rechargement compte virtuel");
            
            req = aangaraaPayRequestRepository.save(req);
            log.info("💾 Requête recharge sauvegardée - ID: {}", req.getId());
            
            AangaraaPayResponse resp = new AangaraaPayResponse();
            resp.setCode(statusCode);
            resp.setMessage(apiResponse.getMessage());
            
            if (apiResponse.getData() != null) {
                resp.setReferenceId(apiResponse.getData().getTransaction_id());
            }
            
            AangaraaPayRequest reqRef = new AangaraaPayRequest();
            reqRef.setId(req.getId());
            resp.setAangaraaPayRequest(reqRef);
            
            resp = aangaraaPayResponseRepository.save(resp);
            log.info("💾 Réponse recharge sauvegardée - ID: {}", resp.getId());
        } catch (Exception e) {
            log.error("❌ Erreur sauvegarde Aangaraa: {}", e.getMessage());
            // Cette erreur n'affecte pas la transaction principale
            // Le rechargement continue même si cette sauvegarde échoue
        }
    }

    /**
     * Reçoit la confirmation instantanée
     */
    @Transactional
    public void processWebhook(Map<String, Object> payload) {
        Object payTokenObj = payload.get("payToken");
        String payToken = payTokenObj != null ? payTokenObj.toString() : null;
        String statusFromApi = String.valueOf(payload.getOrDefault("status", "PENDING"));

        log.info("🔔 WEBHOOK RECU - Token: {}, Status: {}", payToken, statusFromApi);

        transactionRepository.findByPayToken(payToken).ifPresent(t -> {
            if ("PENDING".equals(t.getStatut())) {
                updateTransactionStatus(t, statusFromApi);
            } else {
                log.info("⏭ Transaction {} déjà traitée, statut actuel: {}", t.getId(), t.getStatut());
            }
        });
    }

    private void updateTransactionStatus(Transaction transaction, String statusFromApi) {
        log.info("🔄 Mise à jour transaction {} vers statut: {}", transaction.getId(), statusFromApi);

        if ("SUCCESSFUL".equalsIgnoreCase(statusFromApi)) {
            transaction.setStatut("SUCCESSFUL");
            
            // Vérifier le type de transaction pour savoir où créditer
            TypeTransaction type = transaction.getTransactionType();
            
            if (type == TypeTransaction.RECHARGEMENT) {
                // Pour les rechargements : créditer le client
                try {
                    Client client = transaction.getClient();
                    if (client != null) {
                        BigDecimal montant = transaction.getMontant();
                        clientService.crediterSolde(client.getId(), montant);
                        log.info("💰 Client {} crédité de {} XAF pour rechargement", client.getId(), montant);
                    }
                } catch (Exception e) {
                    log.error("❌ Erreur lors du crédit du client pour rechargement: {}", e.getMessage());
                }
            } else {
                // Pour les autres types (PAYMENT_MARCHAND, etc.) : marquer le QR code comme utilisé et créditer le vendeur
                QRCode qrCode = transaction.getQrCode();
                if (qrCode != null) {
                    qrCode.setEstUtilise(true);
                    qrCodeRepository.save(qrCode);
                }
                
                try {
                    Vendeur vendeur = transaction.getQrCode() != null ? transaction.getQrCode().getVendeur() : null;
                    if (vendeur != null) {
                        BigDecimal montantNet = transaction.getMontantNet() != null ? 
                            transaction.getMontantNet() : transaction.getMontant();
                        auteurService.augmenterSolde(vendeur.getId(), montantNet);
                        log.info("💰 Vendeur {} crédité de {} XAF", vendeur.getId(), montantNet);
                    }
                } catch (Exception e) {
                    log.error("❌ Erreur lors du crédit du vendeur: {}", e.getMessage());
                }
            }
        } else if ("FAILED".equalsIgnoreCase(statusFromApi)) {
            transaction.setStatut("FAILED");
        } else {
            transaction.setStatut("PENDING");
        }
        
        transactionRepository.save(transaction);
        log.info("✅ Transaction {} mise à jour vers: {}", transaction.getId(), transaction.getStatut());
    }

    private void validateRequest(InitiatePaymentRequest request) {
        if (request.getQrCodeId() == null) {
            throw new RuntimeException("qrCodeId requis");
        }
        if (request.getMontant() == null || request.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Montant invalide");
        }
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
                auteurService.augmenterSolde(vendeur.getId(), transaction.getMontantNet());
            }
        } catch (Exception e) {
            log.error("Erreur mise à jour solde: {}", e.getMessage());
        }
    }

    private TypeTransaction determineTransactionType(InitiatePaymentRequest request, QRCode qrCode) {
        if (request.getTransactionType() != null && !request.getTransactionType().isBlank()) {
            try {
                return TypeTransaction.valueOf(request.getTransactionType().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                log.warn("Type de transaction non reconnu: {}. Utilisation du type par défaut.", request.getTransactionType());
            }
        }

        if (qrCode != null && qrCode.getUsageType() != null) {
            try {
                return TypeTransaction.valueOf(qrCode.getUsageType().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("UsageType QR code non reconnu: {}. Utilisation du type par défaut.", qrCode.getUsageType());
            }
        }

        if (qrCode != null && qrCode.getVendeur() != null) {
            return TypeTransaction.PAYMENT_MARCHAND;
        }

        return TypeTransaction.RECHARGEMENT;
    }

    /**
     * Prépare le payload pour AangaraaPay
     */
    private Map<String, Object> prepareAangaraaPayload(InitiatePaymentRequest request, QRCode qrCode, Transaction transaction) {
        Map<String, Object> payload = new HashMap<>();
        
        payload.put("amount", request.getMontant().toString());
        payload.put("description", qrCode.getDescription());
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
    
    // ============================================================================
    // SCHEDULERS - Vérification automatique des transactions et retraits PENDING
    // ============================================================================
    
    /**
     * Scheduler: Vérifie les transactions PENDING toutes les 30 secondes
     * - Ignore les transactions avec payToken null (marque FAILED)
     * - Expire automatiquement les transactions > 15 minutes
     * - Vérifie le statut via API Aangaraa après 5 minutes d'attente
     */
    @Scheduled(fixedRate = 30000) // Toutes les 30 secondes
    @Transactional
    public void checkPendingTransactions() {
        log.info("📅 Scheduler: Vérification des transactions PENDING");
        
        try {
            List<Transaction> pendingTransactions = transactionRepository.findByStatut("PENDING");
            log.info("📅 Nombre de transactions PENDING: {}", pendingTransactions.size());
            
            for (Transaction t : pendingTransactions) {
                try {
                    // Calculer l'âge de la transaction
                    long ageMinutes = java.time.Duration.between(t.getDateCreation(), LocalDateTime.now()).toMinutes();
                    
                    log.info("📅 Vérification Transaction ID: {}, payToken: {}, âge: {} min", 
                            t.getId(), t.getPayToken(), ageMinutes);
                    
                    // 1️⃣ IGNORER SI PAYTOKEN NULL - Marquer comme FAILED
                    if (t.getPayToken() == null || t.getPayToken().isBlank()) {
                        log.warn("📅 Transaction {} ignorée car payToken NULL/VIDE, passage en FAILED", t.getId());
                        t.setStatut("FAILED");
                        t.setMessage("PayToken null - transaction invalide");
                        transactionRepository.save(t);
                        continue;
                    }
                    
                    // 2️⃣ EXPIRATION AUTOMATIQUE > 15 MINUTES
                    if (ageMinutes > 15) {
                        log.warn("📅 Transaction {} expirée automatiquement ({} min > 15 min), passage en FAILED", t.getId(), ageMinutes);
                        t.setStatut("FAILED");
                        t.setMessage("Transaction expirée - délai max dépassé");
                        transactionRepository.save(t);
                        continue;
                    }
                    
                    // 3️⃣ ATTENDRE AU MOINS 5 MINUTES AVANT DE VÉRIFIER VIA API
                    if (ageMinutes < 5) {
                        log.info("📅 Transaction {} encore récente ({} min < 5 min), ignorée pour l'instant", t.getId(), ageMinutes);
                        continue;
                    }
                    
                    // 4️⃣ APPEL API AANGARAA POUR VÉRIFIER LE STATUT
                    Map<String, Object> checkBody = new HashMap<>();
                    checkBody.put("payToken", t.getPayToken());
                    checkBody.put("app_key", APP_KEY);
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(checkBody, headers);
                    
                    ResponseEntity<Map> response = restTemplate.exchange(
                            URL_CHECK, HttpMethod.POST, entity, Map.class);
                    
                    Map<String, Object> responseBody = response.getBody();
                    
                    if (responseBody == null) {
                        log.error("📅 Réponse API NULL pour transaction {}", t.getId());
                        continue;
                    }
                    
                    log.info("📅 Réponse API pour transaction {}: {}", t.getId(), responseBody);
                    
                    String status = String.valueOf(responseBody.getOrDefault("status", "UNKNOWN")).toUpperCase();
                    
                    // 5️⃣ TRAITEMENT DU STATUT REÇU
                    switch (status) {
                        case "SUCCESSFUL":
                        case "SUCCESS":
                            t.setStatut("SUCCESSFUL");
                            
                            // Vérifier le type de transaction
                            TypeTransaction typeTrans = t.getTransactionType();
                            
                            // Pour RECHARGEMENT : créditer le client
                            if (typeTrans == TypeTransaction.RECHARGEMENT) {
                                try {
                                    Client client = t.getClient();
                                    if (client != null) {
                                        clientService.crediterSolde(client.getId(), t.getMontant());
                                        log.info("📅 Client {} crédité de {} XAF (RECHARGEMENT)", client.getId(), t.getMontant());
                                    }
                                } catch (Exception e) {
                                    log.error("📅 Erreur lors du crédit du client: {}", e.getMessage());
                                }
                            }
                            // Pour PAYMENT_MARCHAND : marquer QR code et créditer vendeur
                            else if (typeTrans == TypeTransaction.PAYMENT_MARCHAND || typeTrans == TypeTransaction.TRANSFERT_VIRTUEL) {
                                // Marquer le QR code comme utilisé
                                QRCode qrCode = t.getQrCode();
                                if (qrCode != null) {
                                    qrCode.setEstUtilise(true);
                                    qrCodeRepository.save(qrCode);
                                    log.info("📅 QR Code {} marqué comme utilisé", qrCode.getId());
                                }
                                
                                // Créditer le vendeur
                                try {
                                    Vendeur vendeur = t.getQrCode() != null ? t.getQrCode().getVendeur() : null;
                                    if (vendeur != null) {
                                        BigDecimal montantNet = t.getMontantNet() != null ? t.getMontantNet() : t.getMontant();
                                        auteurService.augmenterSolde(vendeur.getId(), montantNet);
                                        log.info("📅 Vendeur {} crédité de {} XAF", vendeur.getId(), montantNet);
                                    }
                                } catch (Exception e) {
                                    log.error("📅 Erreur lors du crédit du vendeur: {}", e.getMessage());
                                }
                            }
                            
                            log.info("📅 Transaction {} validée SUCCESS", t.getId());
                            break;
                            
                        case "FAILED":
                        case "CANCELLED":
                            t.setStatut("FAILED");
                            t.setMessage("Paiement échoué selon Aangaraa");
                            log.info("📅 Transaction {} échouée (status: {})", t.getId(), status);
                            break;
                            
                        case "PENDING":
                            log.info("📅 Transaction {} toujours PENDING", t.getId());
                            break;
                            
                        default:
                            log.warn("📅 Transaction {} statut inconnu: {}", t.getId(), status);
                            break;
                    }
                    
                    transactionRepository.save(t);
                    log.info("📅 Transaction {} mise à jour vers {}", t.getId(), t.getStatut());
                    
                } catch (Exception e) {
                    log.error("📅 Erreur vérification transaction {}: {}", t.getId(), e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("📅 Erreur globale scheduler transactions PENDING: {}", e.getMessage());
        }
        
        log.info("📅 Scheduler transactions PENDING terminé");
    }
    
    /**
     * Scheduler: Vérifie les retraits (Retrait) PENDING toutes les 30 secondes
     * - Ignore les retraits sans referenceId (en attente de première tentative)
     * - Expire automatiquement les retraits > 15 minutes
     * - Vérifie le statut via API Aangaraa après 5 minutes d'attente
     */
    @Scheduled(fixedRate = 30000) // Toutes les 30 secondes
    @Transactional
    public void checkPendingRetraits() {
        log.info("📅 Scheduler: Vérification des retraits PENDING");
        
        try {
            // Récupérer tous les retraits avec statut PENDING
            List<com.fapshi.backend.entity.Retrait> pendingRetraits = auteurRepository.findByStatut("PENDING");
            
            log.info("📅 Nombre de retraits PENDING: {}", pendingRetraits.size());
            
            for (com.fapshi.backend.entity.Retrait retrait : pendingRetraits) {
                try {
                    // Calculer l'âge du retrait
                    long ageMinutes = java.time.Duration.between(retrait.getDateCreation(), LocalDateTime.now()).toMinutes();
                    
                    log.info("📅 Vérification Retrait ID: {}, referenceId: {}, âge: {} min", 
                            retrait.getId(), retrait.getReferenceId(), ageMinutes);
                    
                    // 1️⃣ IGNORER SI REFERENCEID NULL - C'est un retrait pas encore tenté
                    if (retrait.getReferenceId() == null || retrait.getReferenceId().isBlank()) {
                        log.warn("📅 Retrait {} ignoré car referenceId NULL/VIDE - pas encore tenté", retrait.getId());
                        continue;
                    }
                    
                    // 2️⃣ EXPIRATION AUTOMATIQUE > 15 MINUTES
                    if (ageMinutes > 15) {
                        log.warn("📅 Retrait {} expiré automatiquement ({} min > 15 min), passage en FAILED", 
                                retrait.getId(), ageMinutes);
                        retrait.setStatut("FAILED");
                        retrait.setMessage("Retrait expiré - délai max dépassé");
                        auteurRepository.save(retrait);
                        continue;
                    }
                    
                    // 3️⃣ ATTENDRE AU MOINS 5 MINUTES AVANT DE VÉRIFIER VIA API
                    if (ageMinutes < 5) {
                        log.info("📅 Retrait {} encore récent ({} min < 5 min), ignoré pour l'instant", 
                                retrait.getId(), ageMinutes);
                        continue;
                    }
                    
                    // 4️⃣ APPEL API AANGARAA POUR VÉRIFIER LE STATUT DU RETRAIT
                    String paymentMethod = retrait.getOperateur(); // Orange_Cameroon ou MTN_Cameroon
                    
                    // Construire l'URL de vérification
                    String checkUrl = "https://api-production.aangaraa-pay.com/api/v1/check_withdrawal_status/" 
                            + retrait.getReferenceId() + "?payment_method=" + paymentMethod;
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    
                    ResponseEntity<Map> response = restTemplate.exchange(
                            checkUrl, HttpMethod.GET, entity, Map.class);
                    
                    Map<String, Object> responseBody = response.getBody();
                    
                    if (responseBody == null) {
                        log.error("📅 Réponse API NULL pour retrait {}", retrait.getId());
                        continue;
                    }
                    
                    log.info("📅 Réponse API pour retrait {}: {}", retrait.getId(), responseBody);
                    
                    String status = String.valueOf(responseBody.getOrDefault("status", "UNKNOWN")).toUpperCase();
                    
                    // 5️⃣ TRAITEMENT DU STATUT REÇU
                    switch (status) {
                        case "SUCCESSFUL":
                        case "SUCCESS":
                            retrait.setStatut("SUCCESS");
                            retrait.setMessage("Retrait effectué avec succès");
                            
                            // Débiter le solde
                            if (retrait.getClient() != null) {
                                try {
                                    clientService.debiterSolde(retrait.getClient().getId(), retrait.getMontant());
                                    log.info("💰 Client {} débité de {} pour retrait", retrait.getClient().getId(), retrait.getMontant());
                                } catch (Exception e) {
                                    log.error("❌ Erreur débit client: {}", e.getMessage());
                                }
                            } else if (retrait.getVendeur() != null) {
                                try {
                                    auteurService.diminuerSolde(retrait.getVendeur().getId(), retrait.getMontant());
                                    log.info("💰 Vendeur {} débité de {} pour retrait", retrait.getVendeur().getId(), retrait.getMontant());
                                } catch (Exception e) {
                                    log.error("❌ Erreur débit vendeur: {}", e.getMessage());
                                }
                            }
                            
                            log.info("📅 Retrait {} validé SUCCESS", retrait.getId());
                            break;
                            
                        case "FAILED":
                        case "CANCELLED":
                            retrait.setStatut("FAILED");
                            String errorMsg = String.valueOf(responseBody.getOrDefault("message", "Paiement échoué"));
                            retrait.setMessage(errorMsg);
                            log.info("📅 Retrait {} échoué (status: {})", retrait.getId(), status);
                            break;
                            
                        case "PENDING":
                            log.info("📅 Retrait {} toujours PENDING", retrait.getId());
                            break;
                            
                        default:
                            log.warn("📅 Retrait {} statut inconnu: {}", retrait.getId(), status);
                            break;
                    }
                    
                    auteurRepository.save(retrait);
                    log.info("📅 Retrait {} mis à jour vers {}", retrait.getId(), retrait.getStatut());
                    
                } catch (Exception e) {
                    log.error("📅 Erreur vérification retrait {}: {}", retrait.getId(), e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("📅 Erreur globale scheduler retraits PENDING: {}", e.getMessage());
        }
        
        log.info("📅 Scheduler retraits PENDING terminé");
    }
}
