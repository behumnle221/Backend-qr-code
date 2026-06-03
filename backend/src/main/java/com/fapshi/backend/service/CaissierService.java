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
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CaissierService {

    @Autowired
    private CaissierRepository caissierRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private CaissierResponse toResponse(Caissier c) {
        BigDecimal totalVentes = caissierRepository.sumVentesByCaissierId(c.getId());
        int nombreQr = caissierRepository.countQrByCaissierId(c.getId());
        return new CaissierResponse(
                c.getId(),
                c.getNomCaisse(),
                c.getEmail(),
                c.isActif(),
                c.getDateInscription(),
                totalVentes,
                nombreQr
        );
    }

    public CaissierResponse creerCaissier(CaissierRequest request, Vendeur vendeur) {
        if (caissierRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé par un autre compte.");
        }

        Caissier caissier = new Caissier();
        caissier.setNomCaisse(request.getNomCaisse());
        caissier.setEmail(request.getEmail());
        caissier.setPassword(passwordEncoder.encode(request.getPassword()));
        caissier.setNom(request.getNomCaisse());
        caissier.setVendeur(vendeur);
        caissier.setActif(true);

        Caissier saved = caissierRepository.save(caissier);
        return toResponse(saved);
    }

    public List<CaissierResponse> listerCaissiers(Long vendeurId) {
        return caissierRepository.findByVendeurId(vendeurId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CaissierResponse toggleStatutCaissier(Long caissierId, Long vendeurId) {
        Caissier caissier = caissierRepository.findById(caissierId)
                .orElseThrow(() -> new RuntimeException("Caissier non trouvé."));

        if (!caissier.getVendeur().getId().equals(vendeurId)) {
            throw new RuntimeException("Vous n'êtes pas autorisé à modifier cette caisse.");
        }

        caissier.setActif(!caissier.isActif());
        return toResponse(caissierRepository.save(caissier));
    }

    @Transactional
    public void supprimerCaissier(Long caissierId, Long vendeurId) {
        Caissier caissier = caissierRepository.findById(caissierId)
                .orElseThrow(() -> new RuntimeException("Caissier non trouvé."));

        if (!caissier.getVendeur().getId().equals(vendeurId)) {
            throw new RuntimeException("Vous n'êtes pas autorisé à supprimer cette caisse.");
        }

        caissierRepository.delete(caissier);
    }
}
