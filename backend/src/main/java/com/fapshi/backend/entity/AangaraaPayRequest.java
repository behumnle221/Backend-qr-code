package com.fapshi.backend.entity;

import com.fapshi.backend.enums.TypeRequest;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "aangaraa_pay_requests")
@Data
public class AangaraaPayRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;

    private String phoneNumber;

    private String description;
    
    private String operator;

    private String appKey;

    private String returnUrl;

    private String notifyUrl;

    private String deviseId = "XAF";

    @Enumerated(EnumType.STRING)
    private TypeRequest typeRequest;

private LocalDateTime dateCreation = LocalDateTime.now();

    // Relation propriétaire – c'est ÇA qui crée la colonne transaction_id
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "transaction_id", referencedColumnName = "id")
    private Transaction transaction;

    // Relation inverse
    @OneToOne(mappedBy = "aangaraaPayRequest")
    private AangaraaPayResponse response;
    
    // Getters et Setters manuels
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public TypeRequest getTypeRequest() { return typeRequest; }
    public void setTypeRequest(TypeRequest typeRequest) { this.typeRequest = typeRequest; }
}