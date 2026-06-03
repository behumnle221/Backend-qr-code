package com.fapshi.backend.dto.request;

import java.math.BigDecimal;

/**
 * DTO pour le rechargement du compte virtuel du client
 */
public class RechargementRequest {

    private BigDecimal montant;
    private String operator;  // "Orange_Cameroon" ou "MTN_Cameroon"
    private boolean directPayment = true;  // true = paiement direct PIN
    private String telephone;  // Numéro de téléphone du client

    // GETTERS
    public BigDecimal getMontant() { return montant; }
    public String getOperator() { return operator; }
    public boolean isDirectPayment() { return directPayment; }
    public String getTelephone() { return telephone; }

    // SETTERS
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public void setOperator(String operator) { this.operator = operator; }
    public void setDirectPayment(boolean directPayment) { this.directPayment = directPayment; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
}