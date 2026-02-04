package com.fapshi.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_credentials")
@Data
public class ApiCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String appKey;

    private String nomApplication;

    private String environnement; // "TEST" ou "PROD"

    private boolean actif = true;

    private LocalDateTime derniereUtilisation;
}