package com.fapshi.backend.dto.request;


import java.math.BigDecimal;

public class InitiatePaymentRequest {

    private Long qrCodeId;
    private String telephoneClient;     // Numéro que le client veut utiliser pour payer
    private String operator;            // "Orange_Cameroon" ou "MTN_Cameroon"
    private BigDecimal montant;
    private boolean directPayment = true; // true = paiement direct PIN, false = redirection
    private String transactionType;

    // GETTERS
    public Long getQrCodeId() { return qrCodeId; }
    public String getTelephoneClient() { return telephoneClient; }
    public String getOperator() { return operator; }
    public BigDecimal getMontant() { return montant; }
    public boolean isDirectPayment() { return directPayment; }
    public String getTransactionType() { return transactionType; }

    // SETTERS
    public void setQrCodeId(Long qrCodeId) { this.qrCodeId = qrCodeId; }
    public void setTelephoneClient(String telephoneClient) { this.telephoneClient = telephoneClient; }
    public void setOperator(String operator) { this.operator = operator; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public void setDirectPayment(boolean directPayment) { this.directPayment = directPayment; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
}

