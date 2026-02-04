package com.fapshi.backend.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QrCodeSummaryResponse {

    private Long id;
    private String contenu;
    private BigDecimal montant;
    private String description;
    private LocalDateTime dateCreation;
    private LocalDateTime dateExpiration;
    private boolean estUtilise;

    // Constructeur explicite
    public QrCodeSummaryResponse(Long id, String contenu, BigDecimal montant, 
                                  String description, LocalDateTime dateCreation, 
                                  LocalDateTime dateExpiration, boolean estUtilise) {
        this.id = id;
        this.contenu = contenu;
        this.montant = montant;
        this.description = description;
        this.dateCreation = dateCreation;
        this.dateExpiration = dateExpiration;
        this.estUtilise = estUtilise;
    }
}