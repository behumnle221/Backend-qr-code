package com.fapshi.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class QrCodeResponse {

    private Long id;
    private String contenu;
    private BigDecimal montant;
    private String description;
    private LocalDateTime dateExpiration;
    private boolean estUtilise;
    private String qrPayload;        // ← NOUVEAU : le payload à afficher dans le QR

    public QrCodeResponse(Long id, String contenu, BigDecimal montant, String description,
                          LocalDateTime dateExpiration, boolean estUtilise, String qrPayload) {
        this.id = id;
        this.contenu = contenu;
        this.montant = montant;
        this.description = description;
        this.dateExpiration = dateExpiration;
        this.estUtilise = estUtilise;
        this.qrPayload = qrPayload;
    }

    // Getters
    public Long getId() { return id; }
    public String getContenu() { return contenu; }
    public BigDecimal getMontant() { return montant; }
    public String getDescription() { return description; }
    public LocalDateTime getDateExpiration() { return dateExpiration; }
    public boolean isEstUtilise() { return estUtilise; }
    public String getQrPayload() { return qrPayload; }
}