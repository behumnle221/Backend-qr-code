package com.fapshi.backend.controller;

import com.fapshi.backend.dto.request.RechargementRequest;
import com.fapshi.backend.dto.request.RetraitRequest;
import com.fapshi.backend.dto.response.ApiResponse;
import com.fapshi.backend.dto.response.PaymentInitResponse;
import com.fapshi.backend.dto.response.RetraitResponse;
import com.fapshi.backend.dto.response.TransactionDTO;
import com.fapshi.backend.dto.response.TransactionListResponse;
import com.fapshi.backend.entity.Client;
import com.fapshi.backend.entity.Retrait;
import com.fapshi.backend.repository.RetraitRepository;
import com.fapshi.backend.service.AangaraaWithdrawalService;
import com.fapshi.backend.service.ClientService;
import com.fapshi.backend.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/client")
public class ClientController {

    @Autowired
    private ClientService clientService;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private AangaraaWithdrawalService aangaraaWithdrawalService;
    
    @Autowired
    private RetraitRepository retraitRepository;
    
    private static final Logger log = LoggerFactory.getLogger(ClientController.class);

    @GetMapping("/transactions")
    public ResponseEntity<TransactionListResponse> getTransactions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String dateDebut,
            @RequestParam(required = false) String dateFin) {

        String username = authentication.getName();
        Client client = clientService.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        Long clientId = client.getId();

        // Appel au service pour récupérer les transactions paginées
        List<TransactionDTO> transactions = clientService.getHistoriqueTransactions(clientId, page, size, statut, dateDebut, dateFin);

        // Création de la réponse paginée (adaptée si tu as une Page, sinon ajuste)
        TransactionListResponse response = new TransactionListResponse();
        response.setContent(transactions);
        response.setTotalElements(transactions.size());  // Ajuste avec le vrai total si pagination réelle
        response.setTotalPages(1);  // Ajuste avec le vrai nombre de pages
        response.setCurrentPage(page);
        response.setPageSize(size);
        response.setHasNextPage(false);  // Ajuste selon pagination
        response.setHasPreviousPage(page > 0);

        return ResponseEntity.ok(response);
    }
    
    /**
     * Endpoint pour recharger le compte virtuel du client via Aangaraa
     */
    @PostMapping("/recharger")
    public ResponseEntity<PaymentInitResponse> rechargerCompte(
            Authentication authentication,
            @RequestBody RechargementRequest request) {
        
        // Récupérer le username (email) depuis le token JWT
        String username = authentication.getName();
        
        // Récupérer le client par son email
        Client client = clientService.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        
        Long clientId = client.getId();
        
        // Initier le rechargement via Aangaraa
        PaymentInitResponse response = paymentService.initierRechargement(clientId, request);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Endpoint pour consulter le solde virtuel du client
     */
    @GetMapping("/solde")
    public ResponseEntity<BigDecimal> getSolde(Authentication authentication) {
        Long clientId = getClientIdFromAuth(authentication);
        BigDecimal solde = clientService.getSoldeVirtuel(clientId);
        return ResponseEntity.ok(solde);
    }
    
    /**
     * Demander un retrait du solde virtuel du client vers Mobile Money
     * Endpoint : POST /api/client/retraits
     * 
     * Body:
     * {
     *   "montant": 5000,
     *   "operateur": "Orange_Cameroon",
     *   "telephone": "657515280"
     * }
     */
    @PostMapping("/retraits")
    public ResponseEntity<ApiResponse<RetraitResponse>> demanderRetrait(
            Authentication authentication,
            @RequestBody RetraitRequest request) {
        try {
            String username = authentication.getName();
            Client client = clientService.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));
            Long clientId = client.getId();
            
            if (request.getOperateur() == null || (!request.getOperateur().equals("Orange_Cameroon") && !request.getOperateur().equals("MTN_Cameroon"))) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<RetraitResponse>(false, "Opérateur invalide. Utilisez: Orange_Cameroon ou MTN_Cameroon", null));
            }
            
            if (request.getMontant() == null || request.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<RetraitResponse>(false, "Le montant doit être positif", null));
            }
            if (request.getMontant().compareTo(new BigDecimal("10")) < 0) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<RetraitResponse>(false, "Le montant minimum est de 10 XAF", null));
            }
            
            if (request.getTelephone() == null || request.getTelephone().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<RetraitResponse>(false, "Le numéro de téléphone est requis", null));
            }
            
            BigDecimal soldeClient = clientService.getSoldeVirtuel(clientId);
            if (soldeClient.compareTo(request.getMontant()) < 0) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<RetraitResponse>(false, 
                            "Solde virtuel insuffisant. Solde: " + soldeClient + " XAF, demandé: " + request.getMontant() + " XAF", null));
            }
            
            long minutesEcoulees = 6;
            var dernierRetraitOpt = retraitRepository.findLastRetraitByClient(clientId);
            if (dernierRetraitOpt.isPresent()) {
                RetraitResponse dernier = new RetraitResponse();
                dernier.setDateCreation(dernierRetraitOpt.get().getDateCreation());
                long minutes = java.time.temporal.ChronoUnit.MINUTES.between(dernier.getDateCreation(), LocalDateTime.now());
                if (minutes < 5 && ("PENDING".equals(dernier.getStatut()) || "SUCCESS".equals(dernier.getStatut()))) {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse<RetraitResponse>(false, 
                                "Écart minimum de 5min requis entre les retraits. Dernier retrait: il y a " + minutes + "min", null));
                }
                minutesEcoulees = minutes;
            }
            
            Map<String, Object> withdrawalResult = aangaraaWithdrawalService.effectuerRetraitVersMobile(
                request.getTelephone(),
                request.getMontant(),
                request.getOperateur(),
                client.getNom() != null ? client.getNom() : "Client"
            );
            
// Extraire toutes les infos de la réponse Aangaraa
            Object refIdObj = withdrawalResult.get("referenceId");
            String referenceId = refIdObj != null ? refIdObj.toString() : null;
            Object msgObj = withdrawalResult.get("message");
            String message = msgObj != null ? msgObj.toString() : null;
            Object statusObj = withdrawalResult.get("status");
            String status = statusObj != null ? statusObj.toString() : null;
            Object scObj = withdrawalResult.get("statusCode");
            Integer statusCode = scObj != null ? Integer.parseInt(scObj.toString()) : null;
            
            log.info("💰 Résultat retrait client: {}", withdrawalResult);
            
            if (referenceId == null) {
                Object txIdObj = withdrawalResult.get("transactionId");
                referenceId = txIdObj != null ? txIdObj.toString() : null;
            }
            
            if (message == null || message.isBlank()) {
                Object txMsgObj = withdrawalResult.get("txMessage");
                message = txMsgObj != null ? txMsgObj.toString() : null;
            }
            
            // Déterminer le statut : vérifier statusCode 200/201 OU status SUCCESS
            String statut = "PENDING";
            if (statusCode != null && (statusCode == 200 || statusCode == 201)) {
                // statusCode 200/201 = SUCCESS immédiat
                statut = "SUCCESS";
                try {
                    clientService.debiterSolde(clientId, request.getMontant());
                    log.info("💰 Client {} débité de {} (statusCode={})", clientId, request.getMontant(), statusCode);
                } catch (Exception e) {
                    log.error("Erreur lors de la diminution du solde: {}", e.getMessage());
                }
            } else if (Boolean.TRUE.equals(withdrawalResult.get("success")) || 
                       (status != null && ("SUCCESSFUL".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)))) {
                statut = "SUCCESS";
                try {
                    clientService.debiterSolde(clientId, request.getMontant());
                } catch (Exception e) {
                    log.error("Erreur lors de la diminution du solde: {}", e.getMessage());
                }
            } else if (status != null && ("FAILED".equalsIgnoreCase(status) || "ERROR".equalsIgnoreCase(status))) {
                statut = "FAILED";
            }
            
            if (message == null || message.isBlank()) {
                message = (String) withdrawalResult.get("txMessage");
            }
            
            Retrait retrait = new Retrait();
            retrait.setClient(client);
            retrait.setMontant(request.getMontant());
            retrait.setOperateur(request.getOperateur());
            retrait.setStatut(statut);
            retrait.setReferenceId(referenceId);
            retrait.setMessage(message);
            retrait.setDateCreation(LocalDateTime.now());
            retrait.setTelephone(request.getTelephone());
            
            try {
                retrait = retraitRepository.save(retrait);
            } catch (Exception e) {
                log.error("Erreur sauvegarde retrait: {}", e.getMessage());
                retrait = null;
            }
            
            RetraitResponse response = new RetraitResponse(
                    retrait != null ? retrait.getId() : null,
                    request.getMontant(),
                    statut,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    referenceId,
                    request.getOperateur(),
                    message,
                    request.getTelephone()
            );
            
            String messageReponse = Boolean.TRUE.equals(withdrawalResult.get("success")) ? 
                "Retrait effectué avec succès" : "Retrait demandé (statut: " + statut + ")";
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<RetraitResponse>(true, messageReponse, response));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<RetraitResponse>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Récupérer l'historique des retraits du client (paginé)
     * Endpoint : GET /api/client/retraits?page=0&size=10
     */
    @GetMapping("/retraits")
    public ResponseEntity<ApiResponse<Object>> getRetraits(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Long clientId = getClientIdFromAuth(authentication);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<Retrait> retraits = retraitRepository.findByClientIdOrderByDateCreationDesc(clientId, pageable);
            
            List<RetraitResponse> retraitsList = retraits.getContent().stream()
                    .map(r -> new RetraitResponse(
                            r.getId(),
                            r.getMontant(),
                            r.getStatut(),
                            r.getDateCreation(),
                            r.getDateAttempt(),
                            r.getReferenceId(),
                            r.getOperateur(),
                            r.getMessage(),
                            r.getTelephone()))
                    .toList();
            
            Map<String, Object> responseData = new java.util.LinkedHashMap<>();
            responseData.put("content", retraitsList);
            responseData.put("totalElements", retraits.getTotalElements());
            responseData.put("totalPages", retraits.getTotalPages());
            responseData.put("currentPage", retraits.getNumber());
            responseData.put("pageSize", retraits.getSize());
            responseData.put("hasNextPage", retraits.hasNext());
            responseData.put("hasPreviousPage", retraits.hasPrevious());
            
            return ResponseEntity.ok()
                    .body(new ApiResponse<Object>(true, "Retraits récupérés avec succès", responseData));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<Object>(false, "Erreur lors de la récupération: " + e.getMessage(), null));
        }
    }
    
    private Long getClientIdFromAuth(Authentication authentication) {
        String username = authentication.getName();
        return clientService.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"))
                .getId();
    }
    
    /**
     * Synchroniser les retraits PENDING du client (appel manuel)
     * Endpoint : POST /api/client/retraits/sync
     */
    @PostMapping("/retraits/sync")
    public ResponseEntity<ApiResponse<Object>> syncRetraits(Authentication authentication) {
        try {
            Long clientId = getClientIdFromAuth(authentication);
            
            List<Retrait> retraitsPending = retraitRepository.findByClientId(clientId)
                    .stream() 
                    .filter(r -> "PENDING".equals(r.getStatut()))
                    .toList();
            
            log.info("📅 Sync retraits - {} retraits PENDING pour client {}", retraitsPending.size(), clientId);
            
            int successCount = 0;
            int updatedCount = 0;
            
            for (Retrait retrait : retraitsPending) {
                try {
                    if (retrait.getReferenceId() == null || retrait.getReferenceId().isBlank()) {
                        log.warn("⚠️ Retrait {} sans referenceId, ignoré", retrait.getId());
                        continue;
                    }
                    
                    Map<String, Object> statusResult = aangaraaWithdrawalService.checkWithdrawalStatus(
                            retrait.getReferenceId(), retrait.getOperateur());
                    
                    String status = statusResult != null ? (String) statusResult.get("status") : null;
                    Integer statusCode = statusResult.get("statusCode") != null ? (Integer) statusResult.get("statusCode") : null;
                    
                    if (statusCode != null && (statusCode == 200 || statusCode == 201)) {
                        // statusCode 200 ou 201 = SUCCESS
                        retrait.setStatut("SUCCESS");
                        retrait.setMessage("Synchronisé:SUCCESS (statusCode=" + statusCode + ")");
                        try {
                            clientService.debiterSolde(clientId, retrait.getMontant());
                            log.info("💰 Client {} débité de {} pour retrait {}", clientId, retrait.getMontant(), retrait.getId());
                        } catch (Exception e) {
                            log.error("❌ Erreur débit: {}", e.getMessage());
                        }
                        successCount++;
                    } else if (status != null && ("SUCCESSFUL".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status))) {
                        retrait.setStatut("SUCCESS");
                        retrait.setMessage("Synchronisé:SUCCESS");
                        try {
                            clientService.debiterSolde(clientId, retrait.getMontant());
                            log.info("💰 Client {} débité de {} pour retrait {}", clientId, retrait.getMontant(), retrait.getId());
                        } catch (Exception e) {
                            log.error("❌ Erreur débit: {}", e.getMessage());
                        }
                        successCount++;
                    } else if (status != null && ("FAILED".equalsIgnoreCase(status) || "ERROR".equalsIgnoreCase(status))) {
                        retrait.setStatut("FAILED");
                        retrait.setMessage("Synchronisé:FAILED");
                        updatedCount++;
                    }
                    
                    retrait.setDateAttempt(LocalDateTime.now());
                    retraitRepository.save(retrait);
                    
                } catch (Exception e) {
                    log.error("❌ Erreur sync retrait {}: {}", retrait.getId(), e.getMessage());
                }
            }
            
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("totalPending", retraitsPending.size());
            result.put("success", successCount);
            result.put("failed", updatedCount);
            result.put("pending", retraitsPending.size() - successCount - updatedCount);
            
            return ResponseEntity.ok()
                    .body(new ApiResponse<Object>(true, "Sync terminé", result));
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<Object>(false, "Erreur sync: " + e.getMessage(), null));
        }
    }
    /**
     * Vérifier le statut d'un retrait spécifique
     * Endpoint : GET /api/client/retraits/{transactionId}/statut?operateur=Orange_Cameroon
     */
    @GetMapping("/retraits/{transactionId}/statut")
    public ResponseEntity<ApiResponse<Object>> getRetraitStatut(
            @PathVariable String transactionId,
            @RequestParam String operateur) {
        try {
            if (operateur == null || (!operateur.equals("Orange_Cameroon") && !operateur.equals("MTN_Cameroon"))) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Opérateur requis. Utilisez: Orange_Cameroon ou MTN_Cameroon", null));
            }
            
            Map<String, Object> status = aangaraaWithdrawalService.checkWithdrawalStatus(transactionId, operateur);
            
            return ResponseEntity.ok()
                    .body(new ApiResponse<>(status, "Statut récupéré"));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Erreur: " + e.getMessage(), null));
        }
    }
}