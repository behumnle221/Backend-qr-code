package com.fapshi.backend.dto.request;

import java.math.BigDecimal;

/**
 * DTO pour créer un retrait
 */
public class RetraitRequest {
    
    private BigDecimal montant;  // Montant à retirer
    private String operateur;    // Orange_Cameroon ou MTN_Cameroon
    private String telephone;    // Numéro du bénéficiaire (avec ou sans préfixe 237)
    private String username;     // Nom du bénéficiaire (optionnel)
    
    // Constructeurs
    public RetraitRequest() {}

    public RetraitRequest(BigDecimal montant, String operateur) {
        this.montant = montant;
        this.operateur = operateur;
    }

    // Getters et Setters
    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getOperateur() {
        return operateur;
    }

    public void setOperateur(String operateur) {
        this.operateur = operateur;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
