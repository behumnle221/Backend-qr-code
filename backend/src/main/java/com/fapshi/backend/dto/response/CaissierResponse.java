package com.fapshi.backend.dto.response;

import java.time.LocalDateTime;

public class CaissierResponse {
    private Long id;
    private String nomCaisse;
    private String email;
    private boolean actif;
    private LocalDateTime dateInscription;

    public CaissierResponse(Long id, String nomCaisse, String email, boolean actif, LocalDateTime dateInscription) {
        this.id = id;
        this.nomCaisse = nomCaisse;
        this.email = email;
        this.actif = actif;
        this.dateInscription = dateInscription;
    }

    public Long getId() { return id; }
    public String getNomCaisse() { return nomCaisse; }
    public String getEmail() { return email; }
    public boolean isActif() { return actif; }
    public LocalDateTime getDateInscription() { return dateInscription; }
}
