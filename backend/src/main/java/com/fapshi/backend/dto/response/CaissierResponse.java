package com.fapshi.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CaissierResponse {
    private Long id;
    private String nomCaisse;
    private String email;
    private boolean actif;
    private LocalDateTime dateInscription;
    // stats globales
    private BigDecimal totalVentes;
    private int nombreQrGeneres;
    // stats filtrées par période
    private BigDecimal totalVentesPeriode;
    private int nombreQrPayesPeriode;

    public CaissierResponse(Long id, String nomCaisse, String email, boolean actif,
                            LocalDateTime dateInscription,
                            BigDecimal totalVentes, int nombreQrGeneres,
                            BigDecimal totalVentesPeriode, int nombreQrPayesPeriode) {
        this.id = id;
        this.nomCaisse = nomCaisse;
        this.email = email;
        this.actif = actif;
        this.dateInscription = dateInscription;
        this.totalVentes = totalVentes != null ? totalVentes : BigDecimal.ZERO;
        this.nombreQrGeneres = nombreQrGeneres;
        this.totalVentesPeriode = totalVentesPeriode != null ? totalVentesPeriode : BigDecimal.ZERO;
        this.nombreQrPayesPeriode = nombreQrPayesPeriode;
    }

    public Long getId() { return id; }
    public String getNomCaisse() { return nomCaisse; }
    public String getEmail() { return email; }
    public boolean isActif() { return actif; }
    public LocalDateTime getDateInscription() { return dateInscription; }
    public BigDecimal getTotalVentes() { return totalVentes; }
    public int getNombreQrGeneres() { return nombreQrGeneres; }
    public BigDecimal getTotalVentesPeriode() { return totalVentesPeriode; }
    public int getNombreQrPayesPeriode() { return nombreQrPayesPeriode; }
}
