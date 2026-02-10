package com.fapshi.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "configuration_frais")
@Data
public class ConfigurationFrais {

    @Id
    private Long id = 1L; // Toujours une seule ligne

    private BigDecimal tauxPlateforme = new BigDecimal("0.02"); // 2%

    private BigDecimal fraisRetraitFixe = new BigDecimal("100");

    private BigDecimal montantMinimum = new BigDecimal("100");

    private BigDecimal montantMaximum = new BigDecimal("500000");

    // Taux de commission appliqué aux vendeurs (0% pour l'instant, sera changé à 5% quand activé)
    private BigDecimal commissionRate = new BigDecimal("0.00");

    // Getters et setters explicites pour commissionRate
    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    // Getters et setters pour autres fields
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getTauxPlateforme() {
        return tauxPlateforme;
    }

    public void setTauxPlateforme(BigDecimal tauxPlateforme) {
        this.tauxPlateforme = tauxPlateforme;
    }

    public BigDecimal getFraisRetraitFixe() {
        return fraisRetraitFixe;
    }

    public void setFraisRetraitFixe(BigDecimal fraisRetraitFixe) {
        this.fraisRetraitFixe = fraisRetraitFixe;
    }

    public BigDecimal getMontantMinimum() {
        return montantMinimum;
    }

    public void setMontantMinimum(BigDecimal montantMinimum) {
        this.montantMinimum = montantMinimum;
    }

    public BigDecimal getMontantMaximum() {
        return montantMaximum;
    }

    public void setMontantMaximum(BigDecimal montantMaximum) {
        this.montantMaximum = montantMaximum;
    }
}