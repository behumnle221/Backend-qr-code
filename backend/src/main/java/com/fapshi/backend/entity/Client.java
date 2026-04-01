package com.fapshi.backend.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("CLIENT")
@Data
@EqualsAndHashCode(callSuper = true)
public class Client extends User {

    private BigDecimal soldeVirtuel = BigDecimal.ZERO;
    private LocalDateTime derniereMiseAJourSolde;

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

    // Pas de champs supplémentaires spécifiques pour le moment

}

