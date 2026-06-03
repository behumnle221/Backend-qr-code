package com.fapshi.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour la réponse d'un retrait
 */
public class RetraitResponse {
    
    private Long id;
    private BigDecimal montant;
    private String statut;          // PENDING, SUCCESS, FAILED
    private LocalDateTime dateCreation;
    private LocalDateTime dateAttempt;
    private String referenceId;     // Réf Aangaraa
    private String operateur;       // Orange_Cameroon ou MTN_Cameroon
    private String message;         // Message d'erreur si FAILED
    private String telephone;      // Numéro de téléphone du bénéficiaire
    
    // Constructeurs
    public RetraitResponse() {}

    public RetraitResponse(Long id, BigDecimal montant, String statut, LocalDateTime dateCreation, 
                          LocalDateTime dateAttempt, String referenceId, String operateur, String message) {
        this.id = id;
        this.montant = montant;
        this.statut = statut;
        this.dateCreation = dateCreation;
        this.dateAttempt = dateAttempt;
        this.referenceId = referenceId;
        this.operateur = operateur;
        this.message = message;
    }
    
    // Nouveau constructeur avec téléphone
    public RetraitResponse(Long id, BigDecimal montant, String statut, LocalDateTime dateCreation, 
                          LocalDateTime dateAttempt, String referenceId, String operateur, String message, String telephone) {
        this.id = id;
        this.montant = montant;
        this.statut = statut;
        this.dateCreation = dateCreation;
        this.dateAttempt = dateAttempt;
        this.referenceId = referenceId;
        this.operateur = operateur;
        this.message = message;
        this.telephone = telephone;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getStatut() {
        // Normaliser: SUCCESSFUL -> SUCCESS pour le frontend
        if ("SUCCESSFUL".equalsIgnoreCase(statut)) {
            return "SUCCESS";
        }
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDateAttempt() {
        return dateAttempt;
    }

    public void setDateAttempt(LocalDateTime dateAttempt) {
        this.dateAttempt = dateAttempt;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getOperateur() {
        return operateur;
    }

    public void setOperateur(String operateur) {
        this.operateur = operateur;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }
}

