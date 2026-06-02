package com.fapshi.backend.controller;

import com.fapshi.backend.dto.request.CaissierRequest;
import com.fapshi.backend.dto.response.ApiResponse;
import com.fapshi.backend.dto.response.CaissierResponse;
import com.fapshi.backend.entity.Vendeur;
import com.fapshi.backend.service.CaissierService;
import com.fapshi.backend.service.VendeurService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/vendeur/caissiers")
@PreAuthorize("hasRole('VENDEUR')")
public class CaissierController {

    @Autowired
    private CaissierService caissierService;

    @Autowired
    private VendeurService vendeurService;

    private Vendeur getAuthenticatedVendeur() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return vendeurService.findByEmail(username)
                .or(() -> vendeurService.findByTelephone(username))
                .orElseThrow(() -> new RuntimeException("Vendeur non trouvé."));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CaissierResponse>> creerCaissier(@Valid @RequestBody CaissierRequest request) {
        try {
            Vendeur vendeur = getAuthenticatedVendeur();
            CaissierResponse response = caissierService.creerCaissier(request, vendeur);
            return ResponseEntity.ok(new ApiResponse<>(response, "Caisse créée avec succès."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(null, e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CaissierResponse>>> listerCaissiers() {
        Vendeur vendeur = getAuthenticatedVendeur();
        List<CaissierResponse> response = caissierService.listerCaissiers(vendeur.getId());
        return ResponseEntity.ok(new ApiResponse<>(response, "Liste des caisses récupérée."));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<CaissierResponse>> toggleStatutCaissier(@PathVariable Long id) {
        try {
            Vendeur vendeur = getAuthenticatedVendeur();
            CaissierResponse response = caissierService.toggleStatutCaissier(id, vendeur.getId());
            String message = response.isActif() ? "Caisse activée." : "Caisse désactivée.";
            return ResponseEntity.ok(new ApiResponse<>(response, message));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(null, e.getMessage()));
        }
    }
}
