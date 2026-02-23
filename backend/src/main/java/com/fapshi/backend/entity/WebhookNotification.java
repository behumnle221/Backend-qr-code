package com.fapshi.backend.entity;

import com.fapshi.backend.enums.StatutTransaction;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class WebhookNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String payToken;

    @Enumerated(EnumType.STRING)
    private StatutTransaction status;

    private String message;

    private LocalDateTime dateReception = LocalDateTime.now();

    private boolean traite = false;

    private int tentatives = 0;

    private String transactionIdExterne;


    // Getters and Setters
    public Long getId() { return id; }
    
    public void setId(Long id) { this.id = id; }
    
    public String getPayToken() { return payToken; }
    public void setPayToken(String payToken) { this.payToken = payToken; }
    
    public StatutTransaction getStatus() { return status; }
    public void setStatus(StatutTransaction status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getDateReception() { return dateReception; }
    public void setDateReception(LocalDateTime dateReception) { this.dateReception = dateReception; }
    
    public boolean isTraite() { return traite; }
    public void setTraite(boolean traite) { this.traite = traite; }
    
    public int getTentatives() { return tentatives; }
    public void setTentatives(int tentatives) { this.tentatives = tentatives; }
    
    public String getTransactionIdExterne() { return transactionIdExterne; }
    public void setTransactionIdExterne(String transactionIdExterne) { this.transactionIdExterne = transactionIdExterne; }
}