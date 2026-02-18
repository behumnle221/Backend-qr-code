package com.fapshi.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fapshi.backend.dto.request.GenerateQrRequest;
import com.fapshi.backend.dto.request.ProductItemRequest;
import com.fapshi.backend.dto.response.QrCodeResponse;
import com.fapshi.backend.dto.response.QrValidationResponse;
import com.fapshi.backend.entity.QRCode;
import com.fapshi.backend.entity.Vendeur;
import com.fapshi.backend.repository.QRCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QRCodeService {

    @Autowired
    private QRCodeRepository qrCodeRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Durée de validité du QR Code en minutes (fixée côté serveur)
    private static final int QR_CODE_VALIDITY_MINUTES = 5;

    /**
     * Génère un QR Code riche avec la liste des produits.
     * La date d'expiration est calculée côté serveur (5 minutes).
     */
    public QrCodeResponse generateQRCode(GenerateQrRequest request, Vendeur vendeur) {
        if (request.getProducts() == null || request.getProducts().isEmpty()) {
            throw new RuntimeException("Au moins un produit est requis");
        }

        // Calcul du montant total
        BigDecimal total = request.getProducts().stream()
                .map(p -> p.getPrix().multiply(new BigDecimal(p.getQuantite())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ✅ Date d'expiration calculée côté serveur : now + 5 minutes
        LocalDateTime dateExpiration = LocalDateTime.now().plusMinutes(QR_CODE_VALIDITY_MINUTES);

        // Création de l'entité QRCode
        QRCode qrCode = new QRCode();
        qrCode.setVendeur(vendeur);
        qrCode.setMontant(total);
        qrCode.setDescription(request.getDescription() != null ? request.getDescription() : "Panier client");
        qrCode.setDateExpiration(dateExpiration); // ✅ Expiration fiable côté serveur
        qrCode.setContenu("Paiement " + total + " XAF - " + request.getProducts().size() + " produits");
        qrCode.setHash("QR-" + UUID.randomUUID().toString().substring(0, 12));
        qrCode.setEstUtilise(false);

        // Sauvegarde pour obtenir l'ID
        QRCode saved = qrCodeRepository.save(qrCode);

        // Construction du payload JSON qui sera affiché dans le QR Code
        Map<String, Object> payload = new HashMap<>();
        payload.put("qrCodeId", saved.getId());
        payload.put("products", request.getProducts().stream()
                .map(p -> Map.of(
                        "nom", p.getNom(),
                        "prix", p.getPrix(),
                        "quantite", p.getQuantite()
                ))
                .collect(Collectors.toList()));
        payload.put("total", total.toPlainString());
        payload.put("merchant", vendeur.getNom());
        payload.put("timestamp", LocalDateTime.now().toString());

        String qrPayload;
        try {
            qrPayload = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur lors de la génération du payload QR", e);
        }

        // Mise à jour avec le payload
        saved.setQrPayload(qrPayload);
        qrCodeRepository.save(saved);

        return new QrCodeResponse(
                saved.getId(),
                saved.getContenu(),
                saved.getMontant(),
                saved.getDescription(),
                saved.getDateExpiration(),
                saved.isEstUtilise(),
                qrPayload
        );
    }

    // ===================================================================
    // MÉTHODES EXISTANTES (inchangées)
    // ===================================================================

    public QRCode save(QRCode qrCode) {
        return qrCodeRepository.save(qrCode);
    }

    public List<QRCode> findByVendeurId(Long vendeurId) {
        return qrCodeRepository.findByVendeurIdOrderByDateCreationDesc(vendeurId);
    }

    public List<QRCode> findNonUtilises() {
        return qrCodeRepository.findByEstUtiliseFalse();
    }

    public QrValidationResponse validateQrCode(Long qrCodeId) {
        QRCode qrCode = qrCodeRepository.findById(qrCodeId)
                .orElseThrow(() -> new RuntimeException("QR Code non trouvé"));

        LocalDateTime now = LocalDateTime.now();

        if (qrCode.getDateExpiration().isBefore(now)) {
            return new QrValidationResponse(false, "QR Code expiré", null, null, null, null, null, null, qrCode.isEstUtilise());
        }

        if (qrCode.isEstUtilise()) {
            return new QrValidationResponse(false, "QR Code déjà utilisé", null, null, null, null, null, null, true);
        }

        return new QrValidationResponse(
                true,
                "QR Code valide",
                qrCode.getId(),
                qrCode.getMontant(),
                qrCode.getDescription(),
                qrCode.getVendeur().getNom(),
                qrCode.getVendeur().getTelephone(),
                qrCode.getDateExpiration(),
                qrCode.isEstUtilise()
        );
    }

    public void markQrAsUsed(Long qrCodeId, Long vendeurId) {
        QRCode qrCode = qrCodeRepository.findById(qrCodeId)
                .orElseThrow(() -> new RuntimeException("QR Code non trouvé"));

        if (!qrCode.getVendeur().getId().equals(vendeurId)) {
            throw new RuntimeException("Vous n'êtes pas le propriétaire de ce QR Code");
        }

        if (qrCode.isEstUtilise()) {
            throw new RuntimeException("QR Code déjà marqué comme utilisé");
        }

        if (qrCode.getDateExpiration().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("QR Code expiré, impossible à marquer");
        }

        qrCode.setEstUtilise(true);
        qrCodeRepository.save(qrCode);
    }
}

