package com.fapshi.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "qr_codes")
public class QRCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String contenu;

    private String description;  // ← Champ ajouté ici

    private BigDecimal montant;

    private LocalDateTime dateCreation = LocalDateTime.now();

    private LocalDateTime dateExpiration;

    private boolean estUtilise = false;

    private String hash;

    @ManyToOne
    @JoinColumn(name = "vendeur_id")
    private Vendeur vendeur;

    // Getters et Setters manuels
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDateTime dateExpiration) { this.dateExpiration = dateExpiration; }

    public boolean isEstUtilise() { return estUtilise; }
    public void setEstUtilise(boolean estUtilise) { this.estUtilise = estUtilise; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public Vendeur getVendeur() { return vendeur; }
    public void setVendeur(Vendeur vendeur) { this.vendeur = vendeur; }
}