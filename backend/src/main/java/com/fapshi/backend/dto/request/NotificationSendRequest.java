package com.fapshi.backend.dto.request;

/**
 * DTO pour envoi de notification de test
 */
public class NotificationSendRequest {

    private String titre;
    private String message;
    private String type; // RETRAIT_DEMANDE, RETRAIT_SUCCESS, RETRAIT_FAILED, etc.

    // Constructeurs
    public NotificationSendRequest() {}

    public NotificationSendRequest(String titre, String message, String type) {
        this.titre = titre;
        this.message = message;
        this.type = type;
    }

    // GETTERS
    public String getTitre() { return titre; }
    public String getMessage() { return message; }
    public String getType() { return type; }

    // SETTERS
    public void setTitre(String titre) { this.titre = titre; }
    public void setMessage(String message) { this.message = message; }
    public void setType(String type) { this.type = type; }
}
