package com.fapshi.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * DTO pour réponse notification
 */
public class NotificationResponse {

    private Long id;
    private String type;
    private String titre;
    private String message;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateCreation;
    
    private boolean lue;

    // Constructeur par défaut
    public NotificationResponse() {}

    // Constructeur complet
    public NotificationResponse(Long id, String type, String titre, String message, LocalDateTime dateCreation, boolean lue) {
        this.id = id;
        this.type = type;
        this.titre = titre;
        this.message = message;
        this.dateCreation = dateCreation;
        this.lue = lue;
    }

    // GETTERS
    public Long getId() { return id; }
    public String getType() { return type; }
    public String getTitre() { return titre; }
    public String getMessage() { return message; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public boolean isLue() { return lue; }

    // SETTERS
    public void setId(Long id) { this.id = id; }
    public void setType(String type) { this.type = type; }
    public void setTitre(String titre) { this.titre = titre; }
    public void setMessage(String message) { this.message = message; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public void setLue(boolean lue) { this.lue = lue; }
}
