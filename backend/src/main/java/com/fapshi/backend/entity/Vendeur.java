package com.fapshi.backend.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("VENDEUR")
public class Vendeur extends User {

    private String nomCommerce;
    private String adresse;

    // Solde virtuel du vendeur (somme des montants nets des transactions SUCCESS)
    private BigDecimal soldeVirtuel = BigDecimal.ZERO;

    // Dernière mise à jour du solde (via webhook ou cron)
    private LocalDateTime derniereMiseAJourSolde;

    public String getNomCommerce() {
        return nomCommerce;
    }

    public void setNomCommerce(String nomCommerce) {
        this.nomCommerce = nomCommerce;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public BigDecimal getSoldeVirtuel() {
        return soldeVirtuel;
    }

    public void setSoldeVirtuel(BigDecimal soldeVirtuel) {
        this.soldeVirtuel = soldeVirtuel;
    }

    public LocalDateTime getDerniereMiseAJourSolde() {
        return derniereMiseAJourSolde;
    }

    public void setDerniereMiseAJourSolde(LocalDateTime derniereMiseAJourSolde) {
        this.derniereMiseAJourSolde = derniereMiseAJourSolde;
    }
}