package com.fapshi.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class AangaraaPayResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer code;

    private String payToken;

    private String payUrl;

    private LocalDateTime linkExpireAt;

    private String status;

    private String message;

    private String referenceId;

    private LocalDateTime dateReception = LocalDateTime.now();

    @OneToOne
    @JoinColumn(name = "request_id")
    private AangaraaPayRequest aangaraaPayRequest;
    
    // Getters et Setters manuels
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public LocalDateTime getDateReception() { return dateReception; }
    public void setDateReception(LocalDateTime dateReception) { this.dateReception = dateReception; }
    public AangaraaPayRequest getAangaraaPayRequest() { return aangaraaPayRequest; }
    public void setAangaraaPayRequest(AangaraaPayRequest aangaraaPayRequest) { this.aangaraaPayRequest = aangaraaPayRequest; }
}