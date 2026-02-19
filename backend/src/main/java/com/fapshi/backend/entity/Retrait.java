package com.fapshi.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité représentant un retrait de fonds par un vendeur
 * Un vendeur peut retirer son solde virtuel vers son compte mobile money
 */
@Entity
@Table(name = "retraits")
public class Retrait {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendeur_id", nullable = false)
    private Vendeur vendeur;
    
    @Column(nullable = false)
    private BigDecimal montant;  // Montant à retirer
    
    @Column(nullable = false)
    private String statut = "PENDING";  // PENDING, SUCCESS, FAILED
    
    @Column(nullable = false)
    private LocalDateTime dateCreation;
    
    @Column
    private LocalDateTime dateAttempt;  // Date de la dernière tentative
    
    @Column
    private String referenceId;  // Référence Aangaraa (si SUCCESS)
    
    @Column
    private String operateur;  // Orange_Cameroon ou MTN_Cameroon
    
    @Column
    private String message;  // Message d'erreur si FAILED
    
    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
    
    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Vendeur getVendeur() {
        return vendeur;
    }

    public void setVendeur(Vendeur vendeur) {
        this.vendeur = vendeur;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
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

    public LocalDateTime getDateAttempt() {
        return dateAttempt;
    }

    public void setDateAttempt(LocalDateTime dateAttempt) {
        this.dateAttempt = dateAttempt;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getOperateur() {
        return operateur;
    }

    public void setOperateur(String operateur) {
        this.operateur = operateur;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
