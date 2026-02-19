package com.fapshi.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour représenter une transaction dans la liste d'historique
 */
public class TransactionDTO {
    
    private Long id;
    private BigDecimal montant;              // Montant brut
    private BigDecimal commissionAppliquee;  // Frais retenus
    private BigDecimal montantNet;           // Montant reçu par vendeur
    private String statut;                    // SUCCESS, PENDING, FAILED
    private LocalDateTime dateCreation;
    private String qrcodeId;
    private String clientTelephone;           // Anonymisé (237***3456)
    private String description;               // Description du QR code

    // Constructeurs
    public TransactionDTO() {}

    public TransactionDTO(Long id, BigDecimal montant, BigDecimal commissionAppliquee, 
                         BigDecimal montantNet, String statut, LocalDateTime dateCreation, 
                         String qrcodeId, String clientTelephone, String description) {
        this.id = id;
        this.montant = montant;
        this.commissionAppliquee = commissionAppliquee;
        this.montantNet = montantNet;
        this.statut = statut;
        this.dateCreation = dateCreation;
        this.qrcodeId = qrcodeId;
        this.clientTelephone = clientTelephone;
        this.description = description;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public BigDecimal getCommissionAppliquee() {
        return commissionAppliquee;
    }

    public void setCommissionAppliquee(BigDecimal commissionAppliquee) {
        this.commissionAppliquee = commissionAppliquee;
    }

    public BigDecimal getMontantNet() {
        return montantNet;
    }

    public void setMontantNet(BigDecimal montantNet) {
        this.montantNet = montantNet;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getQrcodeId() {
        return qrcodeId;
    }

    public void setQrcodeId(String qrcodeId) {
        this.qrcodeId = qrcodeId;
    }

    public String getClientTelephone() {
        return clientTelephone;
    }

    public void setClientTelephone(String clientTelephone) {
        this.clientTelephone = clientTelephone;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
