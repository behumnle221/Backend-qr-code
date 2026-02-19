package com.fapshi.backend.dto.request;


import java.math.BigDecimal;

public class InitiatePaymentRequest {

    private Long qrCodeId;
    private String telephoneClient;     // Numéro que le client veut utiliser pour payer
    private String operator;            // "Orange_Cameroon" ou "MTN_Cameroon"
    private BigDecimal montant;
    private boolean directPayment = true; // true = paiement direct PIN, false = redirection

    // GETTERS
    public Long getQrCodeId() { return qrCodeId; }
    public String getTelephoneClient() { return telephoneClient; }
    public String getOperator() { return operator; }
    public BigDecimal getMontant() { return montant; }
    public boolean isDirectPayment() { return directPayment; }

    // SETTERS
    public void setQrCodeId(Long qrCodeId) { this.qrCodeId = qrCodeId; }
    public void setTelephoneClient(String telephoneClient) { this.telephoneClient = telephoneClient; }
    public void setOperator(String operator) { this.operator = operator; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public void setDirectPayment(boolean directPayment) { this.directPayment = directPayment; }
}

