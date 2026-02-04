package com.fapshi.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réponse après connexion réussie : token JWT + infos utilisateur.
 */
public class LoginResponse {   
    private String token;
    private String type = "Bearer";
    private Long userId;
    private String email;
    private String telephone;
    private String role;

    // Constructeur par défaut (Lombok)
    public LoginResponse() {}

    // Constructeur avec tous les champs (pour matcher ton appel)
    public LoginResponse(String token, Long userId, String email, String telephone, String role) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.telephone = telephone;
        this.role = role;
        this.type = "Bearer";
    }

    // Getters et Setters (si pas Lombok ou pour être sûr)
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
