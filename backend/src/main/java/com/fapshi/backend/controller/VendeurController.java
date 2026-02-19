package com.fapshi.backend.controller;

import com.fapshi.backend.dto.request.RetraitRequest;
import com.fapshi.backend.dto.response.ApiResponse;
import com.fapshi.backend.dto.response.RetraitResponse;
import com.fapshi.backend.dto.response.TransactionListResponse;
import com.fapshi.backend.dto.response.VendeurSoldeResponse;
import com.fapshi.backend.entity.Retrait;
import com.fapshi.backend.entity.Vendeur;
import com.fapshi.backend.repository.RetraitRepository;
import com.fapshi.backend.service.AangaraaWithdrawalService;
import com.fapshi.backend.service.VendeurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.data.domain.Page;

/**
 * Controller pour les endpoints Vendeur
 * Endpoints protégés : authentification JWT + rôle VENDEUR requis
 */
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/vendeur")
public class VendeurController {

    @Autowired
    private VendeurService vendeurService;

    @Autowired
    private AangaraaWithdrawalService aangaraaWithdrawalService;

    @Autowired
    private RetraitRepository retraitRepository;

    private static final Logger log = LoggerFactory.getLogger(VendeurController.class);

    /**
     * Récupère le solde virtuel du vendeur connecté
     * Endpoint : GET /api/vendeur/solde
     * Authentification : JWT + rôle VENDEUR
     * 
     * Retourne :
     * {
     *   "success": true,
     *   "message": "Solde récupéré avec succès",
     *   "data": {
     *     "solde": 5500.00,
     *     "devise": "XAF",
     *     "derniereMiseAJour": "2026-02-10 14:30:00",
     *     "message": "Solde mis à jour avec succès"
     *   },
     *   "timestamp": "2026-02-10 14:35:00"
     * }
     */
    @Operation(
        summary = "Récupérer le solde virtuel du vendeur",
        description = "Retourne le solde virtuel personnel du vendeur connecté (somme des montants nets SUCCESS)"
    )
    @GetMapping("/solde")
    @PreAuthorize("hasAuthority('ROLE_VENDEUR')")
    public ResponseEntity<ApiResponse<VendeurSoldeResponse>> getSolde() {
        try {
            // Récupérer le vendeur connecté depuis le token JWT
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName(); // email ou téléphone du vendeur
            
            Vendeur vendeur = vendeurService.findByEmail(username)
                    .or(() -> vendeurService.findByTelephone(username))
                    .orElseThrow(() -> new RuntimeException("Vendeur non trouvé. Veuillez vous reconnecter."));
            
            // Construire la réponse avec solde et dernière mise à jour
            VendeurSoldeResponse soldeResponse = new VendeurSoldeResponse(
                    vendeur.getSoldeVirtuel(),
                    vendeur.getDerniereMiseAJourSolde(),
                    "Solde mis à jour avec succès"
            );
            
            return ResponseEntity.ok()
                    .body(new ApiResponse<>(
                            soldeResponse,
                            "Solde récupéré avec succès"
                    ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(
                            "Erreur lors de la récupération du solde: " + e.getMessage()
                    ));
        }
    }

    /**
     * Endpoint pour recalculer le solde (utile pour les tests ou après retrait)
     * Endpoint : PUT /api/vendeur/recalculer-solde
     * Authentification : JWT + rôle VENDEUR
     */
    @Operation(
        summary = "Recalculer le solde du vendeur",
        description = "Force le recalcul du solde virtuel en cumulant toutes les transactions SUCCESS"
    )
    @PutMapping("/recalculer-solde")
    @PreAuthorize("hasAuthority('ROLE_VENDEUR')")
    public ResponseEntity<ApiResponse<VendeurSoldeResponse>> recalculerSolde() {
        try {
            // Récupérer le vendeur connecté
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            Vendeur vendeur = vendeurService.findByEmail(username)
                    .or(() -> vendeurService.findByTelephone(username))
                    .orElseThrow(() -> new RuntimeException("Vendeur non trouvé. Veuillez vous reconnecter."));
            
            // Forcer la mise à jour du solde
            Vendeur vendeurMisAJour = vendeurService.mettreAJourSolde(vendeur.getId());
            
            VendeurSoldeResponse soldeResponse = new VendeurSoldeResponse(
                    vendeurMisAJour.getSoldeVirtuel(),
                    vendeurMisAJour.getDerniereMiseAJourSolde(),
                    "Solde recalculé avec succès"
            );
            
            return ResponseEntity.ok()
                    .body(new ApiResponse<>(
                            soldeResponse,
                            "Solde recalculé avec succès"
                    ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(
                            "Erreur lors du recalcul du solde: " + e.getMessage()
                    ));
        }
    }

    /**
     * Récupère l'historique des transactions du vendeur avec pagination et filtres
     * Endpoint : GET /api/vendeur/transactions?page=0&size=10&statut=SUCCESS&dateDebut=2026-01-01&dateFin=2026-02-10
     * Authentification : JWT + rôle VENDEUR
     */
    @Operation(
        summary = "Récupérer l'historique des transactions",
        description = "Liste toutes les transactions du vendeur avec pagination et filtres optionnels"
    )
    @GetMapping("/transactions")
    @PreAuthorize("hasAuthority('ROLE_VENDEUR')")
    public ResponseEntity<ApiResponse<TransactionListResponse>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin
    ) {
        try {
            // Récupérer le vendeur connecté
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            Vendeur vendeur = vendeurService.findByEmail(username)
                    .or(() -> vendeurService.findByTelephone(username))
                    .orElseThrow(() -> new RuntimeException("Vendeur non trouvé"));
            
            // Créer la pagination
            Pageable pageable = PageRequest.of(page, size);
            
            // Récupérer les transactions avec filtres
            TransactionListResponse transactions = vendeurService.getTransactions(
                    vendeur.getId(),
                    statut,
                    dateDebut,
                    dateFin,
                    pageable
            );
            
            return ResponseEntity.ok()
                    .body(new ApiResponse<>(
                            transactions,
                            "Transactions récupérées avec succès"
                    ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(
                            "Erreur lors de la récupération des transactions: " + e.getMessage()
                    ));
        }
    }

    /**
     * Exporte les transactions du vendeur au format CSV
     * Endpoint : GET /api/vendeur/transactions/export-csv
     * Authentification : JWT + rôle VENDEUR
     * 
     * Les numéros de téléphone sont anonymisés (237***3456)
     */
    @Operation(
        summary = "Exporter les transactions en CSV",
        description = "Télécharge un fichier CSV avec toutes les transactions (numéros anonymisés)"
    )
    @GetMapping("/transactions/export-csv")
    @PreAuthorize("hasAuthority('ROLE_VENDEUR')")
    public ResponseEntity<String> exportTransactionsToCsv() {
        try {
            // Récupérer le vendeur connecté
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            Vendeur vendeur = vendeurService.findByEmail(username)
                    .or(() -> vendeurService.findByTelephone(username))
                    .orElseThrow(() -> new RuntimeException("Vendeur non trouvé"));
            
            // Générer le CSV
            String csvContent = vendeurService.exportTransactionsToCsv(vendeur.getId());
            
            // Retourner comme fichier téléchargeable
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions_" + vendeur.getId() + ".csv\"")
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(csvContent);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'export: " + e.getMessage());
        }
    }

    /**
     * Demande un retrait
     * Endpoint : POST /api/vendeur/retraits
     * Authentification : JWT + rôle VENDEUR
     * 
     * Body:
     * {
     *   "montant": 5000,
     *   "operateur": "Orange_Cameroon",  // ou MTN_Cameroon
     *   "telephone": "657515280"         // numéro du bénéficiaire
     * }
     * 
     * Conditions:
     * - Solde suffisant
     * - Écart minimum 5h depuis le dernier retrait (SUCCESS ou PENDING)
     * 
     * Réponse (Code 201):
     * {
     *   "success": true,
     *   "message": "Retrait effectué avec succès",
     *   "data": {
     *     "id": 1,
     *     "montant": 5000,
     *     "statut": "SUCCESS",
     *     "dateCreation": "2026-02-10 14:30:00",
     *     "operateur": "Orange_Cameroon",
     *     "referenceId": "abc123def456"
     *   }
     * }
     */
    @PostMapping("/retraits")
    @PreAuthorize("hasAuthority('ROLE_VENDEUR')")
    public ResponseEntity<ApiResponse<RetraitResponse>> demanderRetrait(@RequestBody RetraitRequest request) {
        try {
            // Récupérer le vendeur connecté
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            Vendeur vendeur = vendeurService.findByEmail(username)
                    .or(() -> vendeurService.findByTelephone(username))
                    .orElseThrow(() -> new RuntimeException("Vendeur non trouvé"));
            
            // Valider l'opérateur
            if (!request.getOperateur().equals("Orange_Cameroon") && !request.getOperateur().equals("MTN_Cameroon")) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<RetraitResponse>(false, "Opérateur invalide. Utilisez: Orange_Cameroon ou MTN_Cameroon", null));
            }
            
            // Valider le montant
            if (request.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<RetraitResponse>(false, "Le montant doit être positif", null));
            }
            
            // Valider le numéro de téléphone
            if (request.getTelephone() == null || request.getTelephone().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<RetraitResponse>(false, "Le numéro de téléphone est requis", null));
            }
            
            // Vérifier le solde virtuel local
            if (vendeur.getSoldeVirtuel().compareTo(request.getMontant()) < 0) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<RetraitResponse>(false, 
                            "Solde virtuel insuffisant. Solde: " + vendeur.getSoldeVirtuel() + " XAF, demandé: " + request.getMontant() + " XAF", null));
            }
            
            // Vérifier l'écart 5h
            long heuresEcoulees = 6; // Permettre si pas de retrait précédent
            var dernierRetraitOpt = vendeurService.getDernierRetrait(vendeur.getId());
            if (dernierRetraitOpt.isPresent()) {
                RetraitResponse dernier = dernierRetraitOpt.get();
                long heures = java.time.temporal.ChronoUnit.HOURS.between(dernier.getDateCreation(), LocalDateTime.now());
                if (heures < 5 && ("PENDING".equals(dernier.getStatut()) || "SUCCESS".equals(dernier.getStatut()))) {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse<RetraitResponse>(false, 
                                "Écart minimum de 5h requis entre les retraits. Dernier retrait: il y a " + heures + "h", null));
                }
                heuresEcoulees = heures;
            }
            
            // Appeler l'API AangaraaPay pour effectuer le retrait
            Map<String, Object> withdrawalResult = aangaraaWithdrawalService.effectuerRetraitVersMobile(
                request.getTelephone(),
                request.getMontant(),
                request.getOperateur(),
                vendeur.getNom()
            );
            
            // Créer le retrait en base locale
            String referenceId = (String) withdrawalResult.get("referenceId");
            String message = (String) withdrawalResult.get("message");
            
            // Déterminer le statut
            String statut = "PENDING";
            if (Boolean.TRUE.equals(withdrawalResult.get("success"))) {
                String status = (String) withdrawalResult.get("status");
                if ("SUCCESSFUL".equals(status)) {
                    statut = "SUCCESS";
                    // Déduire du solde virtuel
                    try {
                        vendeurService.diminuerSolde(vendeur.getId(), request.getMontant());
                    } catch (Exception e) {
                        log.error("Erreur lors de la diminution du solde: {}", e.getMessage());
                    }
                } else if ("FAILED".equals(status)) {
                    statut = "FAILED";
                }
            }
            
            // Sauvegarder le retrait
            Retrait retrait = new Retrait();
            retrait.setVendeur(vendeur);
            retrait.setMontant(request.getMontant());
            retrait.setOperateur(request.getOperateur());
            retrait.setStatut(statut);
            retrait.setReferenceId(referenceId);
            retrait.setMessage(message);
            retrait.setDateCreation(LocalDateTime.now());
            
            try {
                retrait = retraitRepository.save(retrait);
            } catch (Exception e) {
                // Repository might not be autowired, use service instead
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
                    message
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
     * Récupère l'historique des retraits du vendeur (paginé)
     * Endpoint : GET /api/vendeur/retraits?page=0&size=10
     * Authentification : JWT + rôle VENDEUR
     * 
     * Réponse (Code 200):
     * {
     *   "success": true,
     *   "message": "Retraits récupérés avec succès",
     *   "data": {
     *     "content": [
     *       {
     *         "id": 1,
     *         "montant": 5000,
     *         "statut": "PENDING",
     *         "dateCreation": "2026-02-10 14:30:00",
     *         "operateur": "Orange_Cameroon"
     *       }
     *     ],
     *     "totalElements": 1,
     *     "totalPages": 1,
     *     "currentPage": 0,
     *     "pageSize": 10
     *   }
     * }
     */
    @GetMapping("/retraits")
    @PreAuthorize("hasAuthority('ROLE_VENDEUR')")
    public ResponseEntity<ApiResponse<Object>> getRetraits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            // Récupérer le vendeur connecté
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            Vendeur vendeur = vendeurService.findByEmail(username)
                    .or(() -> vendeurService.findByTelephone(username))
                    .orElseThrow(() -> new RuntimeException("Vendeur non trouvé"));
            
            // Récupérer les retraits paginés
            Pageable pageable = PageRequest.of(page, size);
            Page<RetraitResponse> retraits = vendeurService.getRetraits(vendeur.getId(), pageable);
            
            // Formatter la réponse
            java.util.Map<String, Object> responseData = new java.util.LinkedHashMap<>();
            responseData.put("content", retraits.getContent());
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

    /**
     * Récupère le solde AangaraaPay (solde réel du compte marchand)
     * Endpoint : GET /api/vendeur/solde-aangaraa
     * Authentification : JWT + rôle VENDEUR
     * 
     * Réponse (Code 200):
     * {
     *   "success": true,
     *   "message": "Solde récupéré avec succès",
     *   "data": {
     *     "totalBalance": 150000.00,
     *     "currency": "XAF",
     *     "operators": [
     *       {"operator": "Orange_Cameroon", "balance": 75000.00, "currency": "XAF"},
     *       {"operator": "MTN_Cameroon", "balance": 75000.00, "currency": "XAF"}
     *     ],
     *     "lastUpdated": "2025-12-29T15:30:00Z"
     *   }
     * }
     */
    @GetMapping("/solde-aangaraa")
    @PreAuthorize("hasAuthority('ROLE_VENDEUR')")
    public ResponseEntity<ApiResponse<Object>> getSoldeAangaraa() {
        try {
            Map<String, Object> balance = aangaraaWithdrawalService.getBalance();
            
            if (Boolean.TRUE.equals(balance.get("success"))) {
                return ResponseEntity.ok()
                        .body(new ApiResponse<>(balance, "Solde AangaraaPay récupéré avec succès"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(new ApiResponse<>(false, (String) balance.get("message"), null));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Erreur lors de la récupération du solde: " + e.getMessage(), null));
        }
    }

    /**
     * Vérifie les informations d'un numéro de téléphone AangaraaPay
     * Endpoint : POST /api/vendeur/verify-phone
     * Authentification : JWT + rôle VENDEUR
     * 
     * Body:
     * {
     *   "telephone": "657515280",
     *   "operateur": "Orange_Cameroon"
     * }
     */
    @PostMapping("/verify-phone")
    @PreAuthorize("hasAuthority('ROLE_VENDEUR')")
    public ResponseEntity<ApiResponse<Object>> verifyPhone(@RequestBody java.util.Map<String, String> request) {
        try {
            String telephone = request.get("telephone");
            String operateur = request.get("operateur");
            
            if (telephone == null || telephone.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Le numéro de téléphone est requis", null));
            }
            
            if (operateur == null || (!operateur.equals("Orange_Cameroon") && !operateur.equals("MTN_Cameroon"))) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Opérateur invalide. Utilisez: Orange_Cameroon ou MTN_Cameroon", null));
            }
            
            Map<String, Object> userInfo = aangaraaWithdrawalService.getUserInfo(telephone, operateur);
            
            if (Boolean.TRUE.equals(userInfo.get("success"))) {
                return ResponseEntity.ok()
                        .body(new ApiResponse<>(userInfo, "Utilisateur trouvé"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, (String) userInfo.get("message"), null));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Erreur lors de la vérification: " + e.getMessage(), null));
        }
    }

    /**
     * Vérifie le statut d'un retrait
     * Endpoint : GET /api/vendeur/retraits/{transactionId}/statut?operateur=Orange_Cameroon
     * Authentification : JWT + rôle VENDEUR
     */
    @GetMapping("/retraits/{transactionId}/statut")
    @PreAuthorize("hasAuthority('ROLE_VENDEUR')")
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
