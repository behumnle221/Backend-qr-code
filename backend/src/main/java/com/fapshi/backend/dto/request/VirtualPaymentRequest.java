package com.fapshi.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * DTO pour les paiements internes par solde virtuel
 * Simplifié : sans besoin de téléphone, opérateur, etc.
 */
public class VirtualPaymentRequest {

    @NotNull(message = "L'ID du QR Code est requis")
    private Long qrCodeId;

    @NotNull(message = "Le montant est requis")
    @Positive(message = "Le montant doit être positif")
    private BigDecimal montant;

    // GETTERS
    public Long getQrCodeId() { return qrCodeId; }
    public BigDecimal getMontant() { return montant; }

    // SETTERS
    public void setQrCodeId(Long qrCodeId) { this.qrCodeId = qrCodeId; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
}
