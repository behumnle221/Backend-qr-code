package com.fapshi.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Classe générique pour standardiser toutes les réponses API.
 * Respecte le principe SOLID : Single Responsibility (une seule responsabilité : envelopper les réponses).
 * Permet d'avoir un format cohérent dans Swagger.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp = LocalDateTime.now();

    // Constructeur pour réponse réussie
    public ApiResponse(T data, String message) {
        this.success = true;
        this.message = message;
        this.data = data;
    }

    // Constructeur pour erreur
    public ApiResponse(String message) {
        this.success = false;
        this.message = message;
    }
}