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
            Map<String, Object> response = restTemplate.getForObject(URL_BALANCE + appKey, Map.class);
            return response != null && response.get("data") != null;
        } catch (Exception e) {
            log.error("Service AangaraaPay non disponible: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Récupère le solde total et par opérateur depuis AangaraaPay
     * 
     * @return Map contenant le solde total et les détails par opérateur
     */
    public Map<String, Object> getBalance() {
        try {
            log.info("Appel API balance avec appKey: {}", appKey);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                URL_BALANCE + appKey,
                HttpMethod.GET,
                null,
                Map.class
            );
            
            if (response.getBody() != null) {
                Map<String, Object> result = new HashMap<>();
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                
                if (data != null) {
                    result.put("success", true);
                    result.put("totalBalance", data.get("total_balance"));
                    result.put("currency", data.get("currency"));
                    result.put("operators", data.get("operators"));
                    result.put("lastUpdated", data.get("last_updated"));
                    log.info("Solde récupéré: {} {}", data.get("total_balance"), data.get("currency"));
                    return result;
                }
            }
            
            return Map.of("success", false, "message", "Réponse invalide");
            
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du solde: {}", e.getMessage());
            return Map.of("success", false, "message", "Erreur: " + e.getMessage());
        }
    }

    /**
     * Récupère les informations d'un utilisateur à partir de son numéro de téléphone
     * 
     * @param phoneNumber Numéro de téléphone (sans préfixe 237)
     * @param operator Opérateur (Orange_Cameroon ou MTN_Cameroon)
     * @return Map contenant les infos utilisateur
     */
    public Map<String, Object> getUserInfo(String phoneNumber, String operator) {
        try {
            // Nettoyer le numéro
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
            
            ResponseEntity<Map> response = restTemplate.exchange(
                URL_GET_USER,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            if (response.getBody() != null) {
                Map<String, Object> result = new HashMap<>();
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

    /**
     * Effectue un retrait vers un compte mobile money
     * 
     * @param phoneNumber Numéro du bénéficiaire (avec ou sans préfixe 237)
     * @param amount Montant à envoyer
     * @param paymentMethod Méthode de paiement (Orange_Cameroon ou MTN_Cameroon)
     * @param username Nom du bénéficiaire (optionnel)
     * @return Map contenant le résultat du retrait
     */
    public Map<String, Object> performWithdrawal(String phoneNumber, BigDecimal amount, 
                                                  String paymentMethod, String username) {
        try {
            // Nettoyer le numéro
            String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");
            if (!cleanPhone.startsWith("237")) {
                cleanPhone = "237" + cleanPhone;
            }
            
            Map<String, Object> body = new HashMap<>();
            body.put("app_key", appKey);
            body.put("phone_number", cleanPhone);
            body.put("amount", amount.toPlainString());
            body.put("payment_method", paymentMethod);
            body.put("username", username != null ? username : "");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            log.info("Appel API withdrawal - Phone: {}, Amount: {}, Method: {}", 
                     cleanPhone, amount, paymentMethod);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                URL_WITHDRAWAL,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            if (response.getBody() != null) {
                Map<String, Object> result = new HashMap<>();
                Map<String, Object> data = response.getBody();
                
                // Analyser la réponse
                Integer statusCode = (Integer) data.get("statusCode");
                String status = (String) data.get("status");
                
                result.put("success", statusCode != null && statusCode == 200);
                result.put("statusCode", statusCode);
                result.put("status", status);
                result.put("message", data.get("message"));
                
                if (data.get("data") != null) {
                    Map<String, Object> txData = (Map<String, Object>) data.get("data");
                    result.put("referenceId", txData.get("reference_id"));
                    result.put("transactionId", txData.get("transaction_id"));
                    result.put("amount", txData.get("amount"));
                    result.put("phoneNumber", txData.get("phone_number"));
                    result.put("paymentMethod", txData.get("payment_method"));
                    result.put("txMessage", txData.get("message"));
                    
                    log.info("Retrait effectué - Status: {}, Reference: {}", 
                             status, txData.get("reference_id"));
                }
                
                return result;
            }
            
            return Map.of("success", false, "message", "Réponse invalide");
            
        } catch (Exception e) {
            log.error("Erreur lors du retrait: {}", e.getMessage());
            return Map.of("success", false, "message", "Erreur: " + e.getMessage());
        }
    }

    /**
     * Vérifie le statut d'un retrait
     * 
     * @param transactionId ID de transaction (reference_id retourné lors du retrait)
     * @param paymentMethod Méthode de paiement (Orange_Cameroon ou MTN_Cameroon)
     * @return Map contenant le statut du retrait
     */
    public Map<String, Object> checkWithdrawalStatus(String transactionId, String paymentMethod) {
        try {
            String url = URL_CHECK_STATUS + transactionId + "?payment_method=" + paymentMethod;
            
            log.info("Vérification statut retrait - TransactionID: {}, Method: {}", 
                     transactionId, paymentMethod);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                Map.class
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

    /**
     * Effectue un retrait pour un vendeur
     * Note: La validation du solde virtuel se fait AVANT d'appeler cette méthode
     * Cette méthode appelle l'API AangaraaPay pour effectuer le transfert réel
     * 
     * @param phoneNumber Numéro de téléphone du bénéficiaire
     * @param amount Montant à retirer
     * @param operator Opérateur (Orange_Cameroon ou MTN_Cameroon)
     * @param username Nom du bénéficiaire (optionnel)
     * @return Map contenant le résultat
     */
    public Map<String, Object> effectuerRetraitVersMobile(String phoneNumber, 
                                                        BigDecimal amount, 
                                                        String operator, 
                                                        String username) {
        
        // Appeler l'API AangaraaPay pour effectuer le retrait
        return performWithdrawal(phoneNumber, amount, operator, username);
    }
}
