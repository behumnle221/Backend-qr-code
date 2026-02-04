package com.fapshi.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO pour la requête de connexion (login).
 */
public class LoginRequest {

    @NotBlank(message = "L'identifiant est obligatoire")
    private String emailOrPhone;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;

    // Getters et Setters manuels (obligatoires si pas Lombok ou Lombok non détecté)
    public String getEmailOrPhone() {
        return emailOrPhone;
    }

    public void setEmailOrPhone(String emailOrPhone) {
        this.emailOrPhone = emailOrPhone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}