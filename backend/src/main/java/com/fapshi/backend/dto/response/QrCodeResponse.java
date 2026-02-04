package com.fapshi.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class QrCodeResponse {

    private Long qrCodeId;
    private String contenu;
    private BigDecimal montant;
    private String description;
    private LocalDateTime dateExpiration;
    private boolean estUtilise;

public QrCodeResponse(Long qrCodeId, String contenu, BigDecimal montant, String description,
                      LocalDateTime dateExpiration, boolean estUtilise) {
    this.qrCodeId = qrCodeId;
    this.contenu = contenu;
    this.montant = montant;
    this.description = description;
    this.dateExpiration = dateExpiration;
    this.estUtilise = estUtilise;
}
    // Getters (manuels pour être sûr)
    public Long getQrCodeId() { return qrCodeId; }
    public String getContenu() { return contenu; }
    public BigDecimal getMontant() { return montant; }
    public LocalDateTime getDateExpiration() { return dateExpiration; }
    public boolean isEstUtilise() { return estUtilise; }

    // Setters si besoin (optionnel)
    public void setQrCodeId(Long qrCodeId) { this.qrCodeId = qrCodeId; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public void setDateExpiration(LocalDateTime dateExpiration) { this.dateExpiration = dateExpiration; }
    public void setEstUtilise(boolean estUtilise) { this.estUtilise = estUtilise; }
}