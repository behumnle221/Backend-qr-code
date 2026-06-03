package com.fapshi.backend.service;

import com.fapshi.backend.dto.request.CaissierRequest;
import com.fapshi.backend.dto.response.CaissierResponse;
import com.fapshi.backend.entity.Caissier;
import com.fapshi.backend.entity.Vendeur;
import com.fapshi.backend.repository.CaissierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CaissierService {

    @Autowired
    private CaissierRepository caissierRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Calcule la date de début selon la période :
     *   JOUR     → début du jour courant (00:00)
     *   SEMAINE  → lundi de la semaine courante
     *   MOIS     → 1er jour du mois courant
     *   TOUT / null → null (pas de filtre de date)
     */
    private LocalDateTime debutPeriode(String periode) {
        if (periode == null) return null;
        LocalDate today = LocalDate.now();
        return switch (periode.toUpperCase()) {
            case "JOUR"    -> today.atStartOfDay();
            case "SEMAINE" -> today.with(DayOfWeek.MONDAY).atStartOfDay();
            case "MOIS"    -> today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
            default        -> null; // TOUT
        };
    }

    private CaissierResponse toResponse(Caissier c, LocalDateTime debut) {
        BigDecimal totalGlobal = caissierRepository.sumVentesByCaissierId(c.getId());
        int nbrQr = caissierRepository.countQrByCaissierId(c.getId());

        BigDecimal totalPeriode;
        int nbrPayesPeriode;
        if (debut != null) {
            totalPeriode    = caissierRepository.sumVentesByCaissierIdSince(c.getId(), debut);
            nbrPayesPeriode = caissierRepository.countQrPayesByCaissierIdSince(c.getId(), debut);
        } else {
            totalPeriode    = totalGlobal;
            nbrPayesPeriode = nbrQr;
        }

        return new CaissierResponse(
                c.getId(), c.getNomCaisse(), c.getEmail(),
                c.isActif(), c.getDateInscription(),
                totalGlobal, nbrQr,
                totalPeriode, nbrPayesPeriode
        );
    }

    public CaissierResponse creerCaissier(CaissierRequest request, Vendeur vendeur) {
        if (caissierRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé par un autre compte.");
        }
        Caissier c = new Caissier();
        c.setNomCaisse(request.getNomCaisse());
        c.setEmail(request.getEmail());
        c.setPassword(passwordEncoder.encode(request.getPassword()));
        c.setNom(request.getNomCaisse());
        c.setVendeur(vendeur);
        c.setActif(true);
        return toResponse(caissierRepository.save(c), null);
    }

    public List<CaissierResponse> listerCaissiers(Long vendeurId, String periode) {
        LocalDateTime debut = debutPeriode(periode);
        return caissierRepository.findByVendeurId(vendeurId).stream()
                .map(c -> toResponse(c, debut))
                .collect(Collectors.toList());
    }

    public CaissierResponse toggleStatutCaissier(Long caissierId, Long vendeurId) {
        Caissier c = caissierRepository.findById(caissierId)
                .orElseThrow(() -> new RuntimeException("Caissier non trouvé."));
        if (!c.getVendeur().getId().equals(vendeurId))
            throw new RuntimeException("Vous n'êtes pas autorisé à modifier cette caisse.");
        c.setActif(!c.isActif());
        return toResponse(caissierRepository.save(c), null);
    }

    @Transactional
    public void supprimerCaissier(Long caissierId, Long vendeurId) {
        Caissier c = caissierRepository.findById(caissierId)
                .orElseThrow(() -> new RuntimeException("Caissier non trouvé."));
        if (!c.getVendeur().getId().equals(vendeurId))
            throw new RuntimeException("Vous n'êtes pas autorisé à supprimer cette caisse.");
        caissierRepository.delete(c);
    }
}
