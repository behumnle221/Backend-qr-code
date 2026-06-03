package com.fapshi.backend.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)   // ← Sécurité future (si Aangaraa ajoute d'autres champs)
public class AangaraaPaymentResponse {

    private int statusCode;
    private String message;
    private Data data;

    // Getters et Setters
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

    @JsonIgnoreProperties(ignoreUnknown = true)   // ← Très important ici
    public static class Data {
        private String payment_url;          // présent en mode redirect
        private String payToken;             // présent en mode direct
        private String transaction_id;
        private Integer payment_history_id;
        private String status;               // ← NOUVEAU : ajouté pour le mode no_redirect

        // Getters et Setters
        public String getPayment_url() { return payment_url; }
        public void setPayment_url(String payment_url) { this.payment_url = payment_url; }

        public String getPayToken() { return payToken; }
        public void setPayToken(String payToken) { this.payToken = payToken; }

        public String getTransaction_id() { return transaction_id; }
        public void setTransaction_id(String transaction_id) { this.transaction_id = transaction_id; }

        public Integer getPayment_history_id() { return payment_history_id; }
        public void setPayment_history_id(Integer payment_history_id) { this.payment_history_id = payment_history_id; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}