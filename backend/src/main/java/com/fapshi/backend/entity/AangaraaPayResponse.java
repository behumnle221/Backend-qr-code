package com.fapshi.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class AangaraaPayResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer code;

    private String payToken;

    private String payUrl;

    private LocalDateTime linkExpireAt;

    private String status;

    private String message;

    private LocalDateTime dateReception = LocalDateTime.now();

    @OneToOne
    @JoinColumn(name = "request_id")
    private AangaraaPayRequest aangaraaPayRequest;
}