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

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        statut = "PENDING";
    }

    // GETTERS MANUELS
    public Long getId() { return id; }
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

    // SETTERS MANUELS
    public void setId(Long id) { this.id = id; }
    public void setQrCode(QRCode qrCode) { this.qrCode = qrCode; }
    public void setTelephoneClient(String telephoneClient) { this.telephoneClient = telephoneClient; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public void setStatut(String statut) { this.statut = statut; }
    public void setPayToken(String payToken) { this.payToken = payToken; }
    public void setPayUrl(String payUrl) { this.payUrl = payUrl; }
    public void setReferenceOperateur(String referenceOperateur) { this.referenceOperateur = referenceOperateur; }
    public void setOperator(String operator) { this.operator = operator; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public void setDateExpiration(LocalDateTime dateExpiration) { this.dateExpiration = dateExpiration; }
}