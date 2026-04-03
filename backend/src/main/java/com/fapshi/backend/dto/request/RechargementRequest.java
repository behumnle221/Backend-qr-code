package com.fapshi.backend.dto.request;

import java.math.BigDecimal;

/**
 * DTO pour le rechargement du compte virtuel du client
 */
public class RechargementRequest {

    private BigDecimal montant;
    private String operator;  // "Orange_Cameroon" ou "MTN_Cameroon"
    private boolean directPayment = true;  // true = paiement direct PIN

    // GETTERS
    public BigDecimal getMontant() { return montant; }
    public String getOperator() { return operator; }
    public boolean isDirectPayment() { return directPayment; }

    // SETTERS
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public void setOperator(String operator) { this.operator = operator; }
    public void setDirectPayment(boolean directPayment) { this.directPayment = directPayment; }
}