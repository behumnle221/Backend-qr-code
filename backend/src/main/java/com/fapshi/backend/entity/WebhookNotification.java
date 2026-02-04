package com.fapshi.backend.entity;

import com.fapshi.backend.enums.StatutTransaction;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class WebhookNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String payToken;

    @Enumerated(EnumType.STRING)
    private StatutTransaction status;

    private String message;

    private LocalDateTime dateReception = LocalDateTime.now();

    private boolean traite = false;

    private int tentatives = 0;

    private String transactionIdExterne;
}