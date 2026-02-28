package com.fapshi.backend.service;

import com.fapshi.backend.repository.VendeurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


import org.springframework.core.ParameterizedTypeReference;


/**
 * Service pour l'intégration avec l'API AangaraaPay
 * Gère : consultation du solde, infos utilisateur, retraits, vérification statut
 */
@Service
public class AangaraaWithdrawalService {

    private static final Logger log = LoggerFactory.getLogger(AangaraaWithdrawalService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private VendeurRepository vendeurRepository;

    // Clé API AangaraaPay (doit être dans les propriétés)
    @Value("${app.aangaraa.api-key:NRYT-9742-EHQY-QB4B}")
    private String appKey;

    // URLs de l'API AangaraaPay
    private static final String URL_BALANCE = "https://api-production.aangaraa-pay.com/api/v1/service/balance/";
    private static final String URL_GET_USER = "https://api-production.aangaraa-pay.com/api/v1/get_user_info";
    private static final String URL_WITHDRAWAL = "https://api-production.aangaraa-pay.com/api/v1/aangaraa-pay/withdrawal";
    private static final String URL_CHECK_STATUS = "https://api-production.aangaraa-pay.com/api/v1/check_withdrawal_status/";

    /**
     * Vérifie si le service AangaraaPay est disponible
     */
    public boolean isServiceAvailable() {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                URL_BALANCE + appKey,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> body = response.getBody();
            return body != null && body.get("data") != null;
        } catch (Exception e) {
            log.error("Service AangaraaPay non disponible: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Récupère le solde total et par opérateur depuis AangaraaPay
     */
    public Map<String, Object> getBalance() {
        try {
            log.info("Appel API balance avec appKey: {}", appKey);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                URL_BALANCE + appKey,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getBody() != null) {
                Map<String, Object> result = new HashMap<>();
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                
                if (data != null) {
                    result.put("success", true);
                    result.put("totalBalance", data.get("balance_in_db"));
                    result.put("currency", "XAF");
                    
                    // Extraire les détails par opérateur
                    @SuppressWarnings("unchecked")
                    Map<String, Object> balanceDetails = (Map<String, Object>) data.get("balance_details");
                    if (balanceDetails != null) {
                        result.put("operators", balanceDetails);
                        
                        // Correction du warning + sécurité
                        Object totalObj = balanceDetails.get("total");
                        if (totalObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> total = (Map<String, Object>) totalObj;
                            if (total != null) {
                                result.put("totalBalance", total.get("amount"));
                                result.put("transactionsCount", total.get("transactions_count"));
                            }
                        }
                    }
                    
                    log.info("Solde récupéré: {} XAF", data.get("balance_in_db"));
                    return result;
                }
            }
            
            return Map.of("success", false, "message", "Réponse invalide");
            
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du solde: {}", e.getMessage());
            return Map.of("success", false, "message", "Erreur: " + e.getMessage());
        }
    }

    // Les autres méthodes restent exactement les mêmes (je les ai gardées intactes)
    public Map<String, Object> getUserInfo(String phoneNumber, String operator) {
        try {
            String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");
            if (cleanPhone.startsWith("237")) {
                cleanPhone = cleanPhone.substring(3);
            }
            
            Map<String, Object> body = new HashMap<>();
            body.put("msisdn", cleanPhone);
            body.put("api_key", appKey);
            body.put("country", "Cameroon");
            body.put("operator", operator);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                URL_GET_USER,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getBody() != null) {
                Map<String, Object> result = new HashMap<>();
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                
                if (data != null) {
                    result.put("success", true);
                    result.put("msisdn", data.get("msisdn"));
                    result.put("userName", data.get("user_name"));
                    result.put("operator", data.get("operator"));
                    result.put("status", data.get("status"));
                    log.info("Utilisateur trouvé: {} ({})", data.get("user_name"), data.get("msisdn"));
                    return result;
                }
            }
            
            return Map.of("success", false, "message", "Utilisateur non trouvé");
            
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des infos utilisateur: {}", e.getMessage());
            return Map.of("success", false, "message", "Erreur: " + e.getMessage());
        }
    }

    public Map<String, Object> performWithdrawal(String phoneNumber, BigDecimal amount, 
                                                  String paymentMethod, String username) {
        try {
            // Nettoyer le numéro: supprimer tous les caractères non numériques
            String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");
            
            // Supprimer le préfixe 237 s'il est présent (AangaraaPay attend le numéro sans préfixe pays)
            if (cleanPhone.startsWith("237")) {
                cleanPhone = cleanPhone.substring(3);
            }
            
            // AangaraaPay pour Orange Money Cameroon attend le numéro sans le préfixe 237
            // Format attendu: 6XXXXXXXX (9 chiffres commençant par 6)
            
            Map<String, Object> body = new HashMap<>();
            body.put("app_key", appKey);
            body.put("phone_number", cleanPhone);
            // Convertir en string sans décimales (ex: "10" au lieu de "10.00")
            
            body.put("amount", String.valueOf(amount.intValue()));

            body.put("payment_method", paymentMethod);
            body.put("username", username != null ? username : "");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            log.info("Appel API withdrawal - Phone: {}, Amount: {}, Method: {}", 
                     cleanPhone, amount, paymentMethod);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                URL_WITHDRAWAL,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getBody() != null) {
                Map<String, Object> result = new HashMap<>();
                Map<String, Object> data = response.getBody();
                
                Integer statusCode = (Integer) data.get("statusCode");
                String status = (String) data.get("status");
                String message = (String) data.get("message");
                
                // Log complet de la réponse pour debug
                log.info("📥 Réponse Aangaraa withdrawal - statusCode: {}, status: {}, message: {}", 
                        statusCode, status, message);
                log.info("📥 Réponse complète: {}", data);
                
                // Déterminer success basé sur statusCode ET status
                boolean isSuccess = (statusCode != null && statusCode == 200) 
                                    || "SUCCESSFUL".equalsIgnoreCase(status)
                                    || "SUCCESS".equalsIgnoreCase(status);
                
                result.put("success", isSuccess);
                result.put("statusCode", statusCode);
                result.put("status", status);
                result.put("message", message);
                
                if (data.get("data") != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> txData = (Map<String, Object>) data.get("data");
                    
                    // Extraire reference_id (plusieurs formats possibles)
                    String refId = (String) txData.get("reference_id");
                    if (refId == null) {
                        refId = (String) txData.get("referenceId");
                    }
                    if (refId == null) {
                        refId = (String) txData.get("referenceId");
                    }
                    
                    String txId = (String) txData.get("transaction_id");
                    if (txId == null) {
                        txId = (String) txData.get("transactionId");
                    }
                    
                    result.put("referenceId", refId);
                    result.put("transactionId", txId);
                    result.put("amount", txData.get("amount"));
                    result.put("phoneNumber", txData.get("phone_number"));
                    result.put("paymentMethod", txData.get("payment_method"));
                    result.put("txMessage", txData.get("message"));
                    
                    // Si le data contient status, l'utiliser
                    if (txData.get("status") != null && result.get("status") == null) {
                        result.put("status", txData.get("status"));
                    }
                    
                    log.info("📥 Retrait - Status: {}, Reference: {}, TransactionId: {}", 
                             status, refId, txId);
                }
                
                return result;
            }
            
            return Map.of("success", false, "message", "Réponse invalide");
            
        } catch (Exception e) {
            log.error("Erreur lors du retrait: {}", e.getMessage());
            return Map.of("success", false, "message", "Erreur: " + e.getMessage());
        }
    }

    public Map<String, Object> checkWithdrawalStatus(String transactionId, String paymentMethod) {
        try {
            String url = URL_CHECK_STATUS + transactionId + "?payment_method=" + paymentMethod;
            
            log.info("Vérification statut retrait - TransactionID: {}, Method: {}", 
                     transactionId, paymentMethod);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getBody() != null) {
                Map<String, Object> result = new HashMap<>();
                Map<String, Object> body = response.getBody();
                
                result.put("success", body.get("success"));
                result.put("status", body.get("status"));
                result.put("operator", body.get("operator"));
                result.put("transactionId", body.get("transaction_id"));
                result.put("amount", body.get("amount"));
                result.put("currency", body.get("currency"));
                result.put("message", body.get("message"));
                result.put("operatorCode", body.get("operator_code"));
                result.put("timestamp", body.get("timestamp"));
                
                if (body.get("details") != null) {
                    result.put("details", body.get("details"));
                }
                
                log.info("Statut du retrait: {} - {}", transactionId, body.get("status"));
                return result;
            }
            
            return Map.of("success", false, "message", "Réponse invalide");
            
        } catch (Exception e) {
            log.error("Erreur lors de la vérification du statut: {}", e.getMessage());
            return Map.of("success", false, "message", "Erreur: " + e.getMessage());
        }
    }

    public Map<String, Object> effectuerRetraitVersMobile(String phoneNumber, 
                                                        BigDecimal amount, 
                                                        String operator, 
                                                        String username) {
        return performWithdrawal(phoneNumber, amount, operator, username);
    }
}