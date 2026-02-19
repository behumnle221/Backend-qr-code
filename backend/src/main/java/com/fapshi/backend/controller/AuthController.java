package com.fapshi.backend.controller;

import com.fapshi.backend.dto.request.ClientRegisterRequest;
import com.fapshi.backend.dto.request.LoginRequest;
import com.fapshi.backend.dto.request.VendeurRegisterRequest;
import com.fapshi.backend.dto.response.ApiResponse;
import com.fapshi.backend.dto.response.LoginResponse;
import com.fapshi.backend.dto.response.UserResponse;
import com.fapshi.backend.entity.Client;
import com.fapshi.backend.entity.User;
import com.fapshi.backend.entity.Vendeur;
import com.fapshi.backend.security.JwtUtil;
import com.fapshi.backend.service.ClientService;
import com.fapshi.backend.service.UserService;
import com.fapshi.backend.service.VendeurService;
import com.fapshi.backend.utils.PasswordEncoderUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.fapshi.backend.dto.request.ForgotPasswordRequest;
import com.fapshi.backend.dto.request.ResetPasswordRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
// @CrossOrigin(origins = "*") //  À sécuriser en production
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private VendeurService vendeurService;

    @Autowired
    private PasswordEncoderUtil passwordEncoderUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    // ───────────────────────────────────────────────
    // INSCRIPTION CLIENT
    // ───────────────────────────────────────────────

    @Operation(summary = "Inscription d'un client", description = "Crée un nouveau compte client")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Client créé avec succès"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données invalides ou identifiants déjà utilisés")
    })
    @PostMapping("/register/client")
    public ResponseEntity<ApiResponse<UserResponse>> registerClient(@Valid @RequestBody ClientRegisterRequest request) {

        if (userService.existsByEmail(request.getEmail()) || userService.existsByTelephone(request.getTelephone())) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>("Email ou téléphone déjà utilisé"));
        }

        Client client = new Client();
        client.setNom(request.getNom());
        client.setEmail(request.getEmail());
        client.setTelephone(request.getTelephone());
        client.setPassword(passwordEncoderUtil.encode(request.getPassword()));

        Client savedClient = clientService.save(client);

        UserResponse response = new UserResponse(
                savedClient.getId(),
                savedClient.getNom(),
                savedClient.getEmail(),
                savedClient.getTelephone(),
                "CLIENT",
                savedClient.getDateInscription()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(response, "Client inscrit avec succès"));
    }

    // ───────────────────────────────────────────────
    // INSCRIPTION VENDEUR
    // ───────────────────────────────────────────────

    @Operation(summary = "Inscription d'un vendeur", description = "Crée un nouveau compte vendeur avec informations du commerce")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Vendeur créé avec succès"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données invalides ou identifiants déjà utilisés")
    })
    @PostMapping("/register/vendeur")
    public ResponseEntity<ApiResponse<UserResponse>> registerVendeur(@Valid @RequestBody VendeurRegisterRequest request) {

        if (userService.existsByEmail(request.getEmail()) || userService.existsByTelephone(request.getTelephone())) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>("Email ou téléphone déjà utilisé"));
        }

        Vendeur vendeur = new Vendeur();
        vendeur.setNom(request.getNom());
        vendeur.setEmail(request.getEmail());
        vendeur.setTelephone(request.getTelephone());
        vendeur.setNomCommerce(request.getNomCommerce());
        vendeur.setAdresse(request.getAdresse());
        vendeur.setPassword(passwordEncoderUtil.encode(request.getPassword()));

        Vendeur savedVendeur = vendeurService.save(vendeur);

        UserResponse response = new UserResponse(
                savedVendeur.getId(),
                savedVendeur.getNom(),
                savedVendeur.getEmail(),
                savedVendeur.getTelephone(),
                "VENDEUR",
                savedVendeur.getDateInscription()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(response, "Vendeur inscrit avec succès"));
    }

    // ───────────────────────────────────────────────
    // CONNEXION (LOGIN) AVEC JWT
    // ───────────────────────────────────────────────

    @Operation(summary = "Connexion utilisateur", description = "Authentifie un utilisateur et retourne un token JWT")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Connexion réussie"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Identifiants incorrects")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {

        try {
            // Authentification avec Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmailOrPhone(),
                            request.getPassword()
                    )
            );

            // Mise en contexte de sécurité
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Récupération des détails utilisateur
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // Récupération de l'utilisateur complet depuis la base
            User user = userService.findByEmail(request.getEmailOrPhone())
                    .orElseGet(() -> userService.findByTelephone(request.getEmailOrPhone())
                            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé")));

            // Détermination du rôle
            String role = user instanceof Client ? "CLIENT" :
                          user instanceof Vendeur ? "VENDEUR" : "ADMIN";

            // Génération du token JWT
            String jwt = jwtUtil.generateToken(
                    userDetails.getUsername(),
                    user.getId(),
                    role
            );

            // Réponse complète
            LoginResponse loginResponse = new LoginResponse(
                    jwt,
                    user.getId(),
                    user.getEmail(),
                    user.getTelephone(),
                    role
            );

            return ResponseEntity.ok(new ApiResponse<>(loginResponse, "Connexion réussie"));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>("Identifiants incorrects"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Erreur lors de la connexion : " + e.getMessage()));
        }
    }

    @Operation(summary = "Demander réinitialisation mot de passe", description = "Envoie un code par email si l'email existe.")
        @PostMapping("/forgot-password")
        public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        userService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(new ApiResponse<>(null, "Si cet email existe, un code de réinitialisation a été envoyé."));
        }

        @Operation(summary = "Réinitialiser mot de passe avec code", description = "Change le password si le code est valide.")
                @PostMapping("/reset-password")
                public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody ResetPasswordRequest request) {
                try {
                        userService.resetPassword(request.getCode(), request.getNewPassword());
                        return ResponseEntity.ok(new ApiResponse<>(null, "Mot de passe réinitialisé avec succès"));
                } catch (RuntimeException e) {
                        return ResponseEntity.badRequest()
                                .body(new ApiResponse<>(null, e.getMessage()));  // ← Message clair au client
                }
                }
}