package com.fapshi.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entité pour stocker les notifications envoyées aux vendeurs
 * Utilisée pour l'historique et Pusher
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vendeur_id", nullable = false)
    private Vendeur vendeur;

    @Column(nullable = false)
    private String type; // RETRAIT_DEMANDE, RETRAIT_SUCCESS, RETRAIT_FAILED, etc.

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @Column(nullable = false)
    private boolean lue = false; // false par défaut (non lue)

    @Column
    private String pusherEventId; // ID retourné par Pusher (si applicable)

    // Constructeur par défaut
    public Notification() {}

    // Constructeur avec paramètres
    public Notification(Vendeur vendeur, String type, String titre, String message) {
        this.vendeur = vendeur;
        this.type = type;
        this.titre = titre;
        this.message = message;
        this.dateCreation = LocalDateTime.now();
        this.lue = false;
    }

    // @PrePersist pour initialiser dateCreation automatiquement
    @PrePersist
    protected void onCreate() {
        if (this.dateCreation == null) {
            this.dateCreation = LocalDateTime.now();
        }
    }

    // GETTERS
    public Long getId() { return id; }
    public Vendeur getVendeur() { return vendeur; }
    public String getType() { return type; }
    public String getTitre() { return titre; }
    public String getMessage() { return message; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public boolean isLue() { return lue; }
    public String getPusherEventId() { return pusherEventId; }

    // SETTERS
    public void setId(Long id) { this.id = id; }
    public void setVendeur(Vendeur vendeur) { this.vendeur = vendeur; }
    public void setType(String type) { this.type = type; }
    public void setTitre(String titre) { this.titre = titre; }
    public void setMessage(String message) { this.message = message; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public void setLue(boolean lue) { this.lue = lue; }
    public void setPusherEventId(String pusherEventId) { this.pusherEventId = pusherEventId; }
}
