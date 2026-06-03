package com.fapshi.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CaissierResponse {
    private Long id;
    private String nomCaisse;
    private String email;
    private boolean actif;
    private LocalDateTime dateInscription;
    private BigDecimal totalVentes;
    private int nombreQrGeneres;

    public CaissierResponse(Long id, String nomCaisse, String email, boolean actif,
                            LocalDateTime dateInscription, BigDecimal totalVentes, int nombreQrGeneres) {
        this.id = id;
        this.nomCaisse = nomCaisse;
        this.email = email;
        this.actif = actif;
        this.dateInscription = dateInscription;
        this.totalVentes = totalVentes != null ? totalVentes : BigDecimal.ZERO;
        this.nombreQrGeneres = nombreQrGeneres;
    }

    public Long getId() { return id; }
    public String getNomCaisse() { return nomCaisse; }
    public String getEmail() { return email; }
    public boolean isActif() { return actif; }
    public LocalDateTime getDateInscription() { return dateInscription; }
    public BigDecimal getTotalVentes() { return totalVentes; }
    public int getNombreQrGeneres() { return nombreQrGeneres; }
}
