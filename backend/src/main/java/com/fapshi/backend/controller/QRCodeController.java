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

    /**
     * Génère un QR Code pour un vendeur connecté.
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('VENDEUR')")
    public ResponseEntity<ApiResponse<QrCodeResponse>> generateQRCode(@Valid @RequestBody GenerateQrRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        Vendeur vendeur = vendeurService.findByEmail(username)
                .or(() -> vendeurService.findByTelephone(username))
                .orElseThrow(() -> new RuntimeException("Vendeur non trouvé."));

        QrCodeResponse response = qrCodeService.generateQRCode(request, vendeur);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(response, "QR Code généré avec succès"));
    }

    /**
     * Liste tous les QR Codes créés par le vendeur connecté.
     */
    @Operation(summary = "Liste des QR Codes du vendeur", description = "Retourne tous les QR Codes créés par le vendeur authentifié.")
    @GetMapping("/my-qrs")
    @PreAuthorize("hasAuthority('VENDEUR')")
    public ResponseEntity<ApiResponse<List<QrCodeSummaryResponse>>> getMyQRCodes() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();  // ← Correction : username (pas usermane)

        Vendeur vendeur = vendeurService.findByEmail(username)
                .or(() -> vendeurService.findByTelephone(username))
                .orElseThrow(() -> new RuntimeException("Vendeur non trouvé. Veuillez vous reconnecter."));

        List<QRCode> qrCodes = qrCodeService.findByVendeurId(vendeur.getId());

        List<QrCodeSummaryResponse> responseList = qrCodes.stream()
                .map(qr -> new QrCodeSummaryResponse(
                        qr.getId(),
                        qr.getContenu(),
                        qr.getMontant(),
                        qr.getDescription(),
                        qr.getDateCreation(),
                        qr.getDateExpiration(),
                        qr.isEstUtilise()
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
    @PreAuthorize("hasAuthority('VENDEUR')")
    public ResponseEntity<ApiResponse<String>> markQrAsUsed(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        Vendeur vendeur = vendeurService.findByEmail(username)
                .or(() -> vendeurService.findByTelephone(username))
                .orElseThrow(() -> new RuntimeException("Vendeur non trouvé. Veuillez vous reconnecter."));

        try {
            qrCodeService.markQrAsUsed(id, vendeur.getId());
            return ResponseEntity.ok(new ApiResponse<>(null, "QR Code marqué comme utilisé avec succès"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(null, e.getMessage()));
        }
    }
}