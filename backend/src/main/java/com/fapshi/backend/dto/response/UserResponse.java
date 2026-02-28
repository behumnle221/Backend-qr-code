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
//@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String nom;
    private String email;
    private String telephone;
    private String type; 
    
    // Décommente ceci et ajoute le formatage
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateInscription;

    // Ton constructeur explicite doit correspondre aux champs
    public UserResponse(Long id, String nom, String email, String telephone, String type, LocalDateTime dateInscription) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.telephone = telephone;
        this.type = type;
        this.dateInscription = dateInscription; // N'oublie pas de l'assigner !
    }
}
