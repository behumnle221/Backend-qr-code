package com.fapshi.backend.controller;

import com.fapshi.backend.dto.request.GenerateQrRequest;
import com.fapshi.backend.dto.response.ApiResponse;
import com.fapshi.backend.dto.response.QrCodeResponse;
import com.fapshi.backend.dto.response.QrCodeSummaryResponse;
import com.fapshi.backend.dto.response.QrValidationResponse;
import com.fapshi.backend.entity.QRCode;
import com.fapshi.backend.entity.Vendeur;
import com.fapshi.backend.service.QRCodeService;
import com.fapshi.backend.service.VendeurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller pour la génération, validation et marquage des QR Codes.
 */
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/qr")
public class QRCodeController {

    @Autowired
    private QRCodeService qrCodeService;

    @Autowired
    private VendeurService vendeurService;

    @Autowired
    private com.fapshi.backend.repository.UserRepository userRepository;

    /**
     * Génère un QR Code pour un vendeur connecté.
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<QrCodeResponse>> generateQRCode(@Valid @RequestBody GenerateQrRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        com.fapshi.backend.entity.User user = userRepository.findByEmail(username)
                .or(() -> userRepository.findByTelephone(username))
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé."));

        Vendeur vendeur;
        com.fapshi.backend.entity.Caissier caissier = null;

        if (user instanceof Vendeur) {
            vendeur = (Vendeur) user;
        } else if (user instanceof com.fapshi.backend.entity.Caissier) {
            caissier = (com.fapshi.backend.entity.Caissier) user;
            vendeur = caissier.getVendeur();
            if (vendeur == null) {
                throw new RuntimeException("Ce caissier n'est lié à aucun vendeur.");
            }
            if (!caissier.isActif()) {
                throw new RuntimeException("Ce compte caissier est désactivé.");
            }
        } else {
            throw new RuntimeException("Seul un vendeur ou un caissier peut générer un QR Code.");
        }

        QrCodeResponse response = qrCodeService.generateQRCode(request, vendeur, caissier);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(response, "QR Code généré avec succès"));
    }

    /**
     * Liste tous les QR Codes créés par le vendeur connecté.
     */
    @Operation(summary = "Liste des QR Codes du vendeur", description = "Retourne tous les QR Codes créés par le vendeur authentifié.")
    @GetMapping("/my-qrs")
    @PreAuthorize("hasAnyRole('VENDEUR', 'CAISSIER')")
    public ResponseEntity<ApiResponse<List<QrCodeSummaryResponse>>> getMyQRCodes() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();  

        com.fapshi.backend.entity.User user = userRepository.findByEmail(username)
                .or(() -> userRepository.findByTelephone(username))
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé."));

        Long vendeurId;
        if (user instanceof Vendeur) {
            vendeurId = user.getId();
        } else if (user instanceof com.fapshi.backend.entity.Caissier) {
            vendeurId = ((com.fapshi.backend.entity.Caissier) user).getVendeur().getId();
        } else {
            throw new RuntimeException("Rôle non autorisé.");
        }

        List<QRCode> qrCodes = qrCodeService.findByVendeurId(vendeurId);

        List<QrCodeSummaryResponse> responseList = qrCodes.stream()
                .map(qr -> new QrCodeSummaryResponse(
                        qr.getId(),
                        qr.getContenu(),
                        qr.getMontant(),
                        qr.getDescription(),
                        qr.getDateCreation(),
                        qr.getDateExpiration(),
                        qr.isEstUtilise(),
                        qr.getQrPayload()
                ))
                .toList();

        return ResponseEntity.ok(new ApiResponse<>(responseList, "Liste des QR Codes récupérée"));
    }

    /**
     * Valide un QR Code lors du scan par un client.
     * Accessible à tous (pas de rôle requis).
     */

    @Operation(summary = "Valider / Scanner un QR Code", description = "Vérifie si le QR est valide, non expiré et non utilisé.")
    @GetMapping("/validate/{qrCodeId}")
    public ResponseEntity<ApiResponse<QrValidationResponse>> validateQrCode(@PathVariable Long qrCodeId) {
        try {
            QrValidationResponse validation = qrCodeService.validateQrCode(qrCodeId);
            return ResponseEntity.ok(new ApiResponse<>(validation, validation.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(null, e.getMessage()));
        }
    }


    /**
     * Marque un QR Code comme utilisé après paiement réussi.
     * Réservé au vendeur propriétaire du QR.
     */

    @Operation(summary = "Marquer un QR Code comme utilisé", description = "Met estUtilise = true après un paiement confirmé. Réservé au vendeur.")
    @PutMapping("/{id}/mark-used")
    @PreAuthorize("hasAnyRole('VENDEUR', 'CAISSIER')")
    public ResponseEntity<ApiResponse<String>> markQrAsUsed(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        com.fapshi.backend.entity.User user = userRepository.findByEmail(username)
                .or(() -> userRepository.findByTelephone(username))
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé."));

        Long vendeurId;
        if (user instanceof Vendeur) {
            vendeurId = user.getId();
        } else if (user instanceof com.fapshi.backend.entity.Caissier) {
            vendeurId = ((com.fapshi.backend.entity.Caissier) user).getVendeur().getId();
        } else {
            throw new RuntimeException("Rôle non autorisé.");
        }

        try {
            qrCodeService.markQrAsUsed(id, vendeurId);
            return ResponseEntity.ok(new ApiResponse<>(null, "QR Code marqué comme utilisé avec succès"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(null, e.getMessage()));
        }
    }
}

