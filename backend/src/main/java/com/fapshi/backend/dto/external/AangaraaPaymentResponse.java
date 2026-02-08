package com.fapshi.backend.dto.external;

public class AangaraaPaymentResponse {

    private int statusCode;
    private String message;
    private Data data;

    // Getters et Setters manuels
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

    public static class Data {
        private String payment_url;          // présent en mode redirect
        private String payToken;             // présent en mode direct
        private String transaction_id;
        private Integer payment_history_id;

        // Getters et Setters manuels
        public String getPayment_url() { return payment_url; }
        public void setPayment_url(String payment_url) { this.payment_url = payment_url; }

        public String getPayToken() { return payToken; }
        public void setPayToken(String payToken) { this.payToken = payToken; }

        public String getTransaction_id() { return transaction_id; }
        public void setTransaction_id(String transaction_id) { this.transaction_id = transaction_id; }

        public Integer getPayment_history_id() { return payment_history_id; }
        public void setPayment_history_id(Integer payment_history_id) { this.payment_history_id = payment_history_id; }
    }
}