package com.fapshi.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de réponse pour renvoyer les infos d'un utilisateur après inscription.
 * Ne contient pas le mot de passe (sécurité).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String nom;
    private String email;
    private String telephone;
    private String type; // "CLIENT" ou "VENDEUR"
    //private LocalDateTime dateInscription ;
     
        // Ajouté : constructeur explicite si Lombok n'est pas actif
    public UserResponse(Long id, String nom, String email, String telephone, String type, LocalDateTime dateInscription) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.telephone = telephone;
        this.type = type;
    }
}

