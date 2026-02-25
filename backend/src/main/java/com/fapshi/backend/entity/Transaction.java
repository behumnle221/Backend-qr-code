package com.fapshi.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ID unique au format TRANS_1769339875485 (TRANS_timestamp)
    
    @Column
    private String transactionId;
    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;  

    @ManyToOne
    @JoinColumn(name = "qr_code_id", nullable = false)
    private QRCode qrCode;

    private String telephoneClient;

    @Column(nullable = false)
    private BigDecimal montant;

    private String statut; // PENDING, SUCCESS, FAILED, EXPIRED, CANCELLED

    private String payToken;

    private String payUrl; // rempli seulement si mode redirection

    private String referenceOperateur;

    private String operator; // Orange_Cameroon, MTN_Cameroon

    private LocalDateTime dateCreation;

    private LocalDateTime dateExpiration;

    // Commission appliquée sur cette transaction (en XAF)
    private BigDecimal commissionAppliquee = BigDecimal.ZERO;

    // Montant net reçu par le vendeur (montant - commission)
    private BigDecimal montantNet;

    // Message d'erreur ou d'information
    private String message;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        statut = "PENDING";
        if (montantNet == null) {
            montantNet = montant; // Par défaut = montant brut si pas de commission
        }
    }

    // GETTERS MANUELS
    public Long getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public QRCode getQrCode() { return qrCode; }
    public String getTelephoneClient() { return telephoneClient; }
    public BigDecimal getMontant() { return montant; }
    public String getStatut() { return statut; }
    public String getPayToken() { return payToken; }
    public String getPayUrl() { return payUrl; }
    public String getReferenceOperateur() { return referenceOperateur; }
    public String getOperator() { return operator; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public LocalDateTime getDateExpiration() { return dateExpiration; }
    public Client getClient() {return client;}
    public BigDecimal getCommissionAppliquee() { return commissionAppliquee; }
    public BigDecimal getMontantNet() { return montantNet; }

    // SETTERS MANUELS
    public void setId(Long id) { this.id = id; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public void setQrCode(QRCode qrCode) { this.qrCode = qrCode; }
    public void setTelephoneClient(String telephoneClient) { this.telephoneClient = telephoneClient; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public void setStatut(String statut) { this.statut = statut; }
    public void setClient(Client client) {this.client = client;}
    public void setPayToken(String payToken) { this.payToken = payToken; }
    public void setPayUrl(String payUrl) { this.payUrl = payUrl; }
    public void setReferenceOperateur(String referenceOperateur) { this.referenceOperateur = referenceOperateur; }
    public void setOperator(String operator) { this.operator = operator; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public void setDateExpiration(LocalDateTime dateExpiration) { this.dateExpiration = dateExpiration; }
    public void setCommissionAppliquee(BigDecimal commissionAppliquee) { this.commissionAppliquee = commissionAppliquee; }
    public void setMontantNet(BigDecimal montantNet) { this.montantNet = montantNet; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}