package com.fapshi.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class StatusCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String payToken;

    private String appKey;

    private String reponseMessage;

    private LocalDateTime dateCheck = LocalDateTime.now();
}