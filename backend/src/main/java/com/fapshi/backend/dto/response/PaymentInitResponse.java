package com.fapshi.backend.dto.response;

public class PaymentInitResponse {

    private boolean success;
    private String message;
    private String payUrl;        // seulement si !directPayment
    private String payToken;
    private Long transactionId;

    // GETTERS
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getPayUrl() { return payUrl; }
    public String getPayToken() { return payToken; }
    public Long getTransactionId() { return transactionId; }

    // SETTERS
    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setPayUrl(String payUrl) { this.payUrl = payUrl; }
    public void setPayToken(String payToken) { this.payToken = payToken; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
}