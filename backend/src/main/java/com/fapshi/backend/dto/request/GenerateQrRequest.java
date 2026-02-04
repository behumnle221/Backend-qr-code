package com.fapshi.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour la requête de génération de QR Code.
 */
public class GenerateQrRequest {

    @NotNull(message = "Le montant est obligatoire")
    @Positive(message = "Le montant doit être positif")
    private BigDecimal montant;

    private String description; // Optionnel

    @NotNull(message = "Date d'expiration obligatoire")
    private LocalDateTime dateExpiration;

    // ───────────────────────────────────────────────
    // GETTERS ET SETTERS MANUELS (obligatoires si Lombok non actif)
    // ───────────────────────────────────────────────
    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(LocalDateTime dateExpiration) {
        this.dateExpiration = dateExpiration;
    }
}