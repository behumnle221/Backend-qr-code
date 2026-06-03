package com.fapshi.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CaissierRequest {

    @NotBlank(message = "Le nom de la caisse est obligatoire.")
    private String nomCaisse;

    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "Format d'email invalide.")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères.")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[0-9]).+$",
             message = "Le mot de passe doit contenir au moins une majuscule et un chiffre.")
    private String password;

    public String getNomCaisse() { return nomCaisse; }
    public void setNomCaisse(String nomCaisse) { this.nomCaisse = nomCaisse; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
