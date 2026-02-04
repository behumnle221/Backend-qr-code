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
}