package com.fapshi.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour retourner le solde virtuel d'un vendeur
 * Affiche uniquement le solde personnel du vendeur (pas le solde global Aangaraa)
 */
public class VendeurSoldeResponse {

    private BigDecimal solde;
    private LocalDateTime derniereMiseAJour;
    private String devise = "XAF";
    private String message;

    // --- CONSTRUCTEURS ---

    public VendeurSoldeResponse() {
    }

    public VendeurSoldeResponse(BigDecimal solde, LocalDateTime derniereMiseAJour) {
        this.solde = solde;
        this.derniereMiseAJour = derniereMiseAJour;
        this.message = "Solde mis à jour avec succès";
    }

    public VendeurSoldeResponse(BigDecimal solde, LocalDateTime derniereMiseAJour, String message) {
        this.solde = solde;
        this.derniereMiseAJour = derniereMiseAJour;
        this.message = message;
    }

    // --- GETTERS ---

    public BigDecimal getSolde() {
        return solde;
    }

    public LocalDateTime getDerniereMiseAJour() {
        return derniereMiseAJour;
    }

    public String getDevise() {
        return devise;
    }

    public String getMessage() {
        return message;
    }

    // --- SETTERS ---

    public void setSolde(BigDecimal solde) {
        this.solde = solde;
    }

    public void setDerniereMiseAJour(LocalDateTime derniereMiseAJour) {
        this.derniereMiseAJour = derniereMiseAJour;
    }

    public void setDevise(String devise) {
        this.devise = devise;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "VendeurSoldeResponse{" +
                "solde=" + solde +
                ", devise='" + devise + '\'' +
                ", derniereMiseAJour=" + derniereMiseAJour +
                ", message='" + message + '\'' +
                '}';
    }
}
