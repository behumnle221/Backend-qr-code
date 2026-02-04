package com.fapshi.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Réponse après validation/scannage d'un QR Code.
 */
public class QrValidationResponse {

    private boolean valide;
    private String message;
    private Long qrCodeId;
    private BigDecimal montant;
    private String description;
    private String vendeurNom;
    private String vendeurTelephone;
    private LocalDateTime dateExpiration;
    private boolean estUtilise;

    // --- CONSTRUCTEURS ---

    // Constructeur sans arguments (nécessaire pour la désérialisation JSON)
    public QrValidationResponse() {
    }

    // Constructeur complet
    public QrValidationResponse(boolean valide, String message, Long qrCodeId, BigDecimal montant, 
                                String description, String vendeurNom, String vendeurTelephone, 
                                LocalDateTime dateExpiration, boolean estUtilise) {
        this.valide = valide;
        this.message = message;
        this.qrCodeId = qrCodeId;
        this.montant = montant;
        this.description = description;
        this.vendeurNom = vendeurNom;
        this.vendeurTelephone = vendeurTelephone;
        this.dateExpiration = dateExpiration;
        this.estUtilise = estUtilise;
    }

    // --- GETTERS ---

    public boolean isValide() { return valide; }
    public String getMessage() { return message; }
    public Long getQrCodeId() { return qrCodeId; }
    public BigDecimal getMontant() { return montant; }
    public String getDescription() { return description; }
    public String getVendeurNom() { return vendeurNom; }
    public String getVendeurTelephone() { return vendeurTelephone; }
    public LocalDateTime getDateExpiration() { return dateExpiration; }
    public boolean isEstUtilise() { return estUtilise; }

    // --- SETTERS ---

    public void setValide(boolean valide) { this.valide = valide; }
    public void setMessage(String message) { this.message = message; }
    public void setQrCodeId(Long qrCodeId) { this.qrCodeId = qrCodeId; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public void setDescription(String description) { this.description = description; }
    public void setVendeurNom(String vendeurNom) { this.vendeurNom = vendeurNom; }
    public void setVendeurTelephone(String vendeurTelephone) { this.vendeurTelephone = vendeurTelephone; }
    public void setDateExpiration(LocalDateTime dateExpiration) { this.dateExpiration = dateExpiration; }
    public void setEstUtilise(boolean estUtilise) { this.estUtilise = estUtilise; }
}