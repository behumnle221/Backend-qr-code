package com.fapshi.backend.service;

import com.fapshi.backend.dto.response.QrValidationResponse;
import com.fapshi.backend.entity.QRCode;
import com.fapshi.backend.repository.QRCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service pour générer et gérer les QR Codes des vendeurs.
 */
@Service
public class QRCodeService {

    @Autowired
    private QRCodeRepository qrCodeRepository;

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

    // ───────────────────────────────────────────────
    // MÉTHODE À PLACER ICI (pas à l’intérieur d’une autre !)
    // ───────────────────────────────────────────────

    public void markQrAsUsed(Long qrCodeId, Long vendeurId) {
        QRCode qrCode = qrCodeRepository.findById(qrCodeId)
                .orElseThrow(() -> new RuntimeException("QR Code non trouvé"));

        // Vérification : seul le vendeur propriétaire peut marquer son QR
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
        qrCodeRepository.save(qrCode);  // ← CORRECTION ICI (repository, pas service)
    }
}