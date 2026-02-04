package com.fapshi.backend.entity;

import com.fapshi.backend.enums.TypeRequest;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "aangaraa_pay_requests")
@Data
public class AangaraaPayRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;

    private String phoneNumber;

    private String description;

    private String appKey;

    // Renommé pour éviter le conflit avec le nom de la relation "transaction"
    private String externalTransactionId; // ID renvoyé par Aangaraa (transaction_id externe)

    private String returnUrl;

    private String notifyUrl;

    private String operator;

    private String deviseId = "XAF";

    @Enumerated(EnumType.STRING)
    private TypeRequest typeRequest;

    private LocalDateTime dateCreation = LocalDateTime.now();

    // Relation propriétaire – c’est ÇA qui crée la colonne transaction_id
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "transaction_id", referencedColumnName = "id")
    private Transaction transaction;

    // Relation inverse
    @OneToOne(mappedBy = "aangaraaPayRequest")
    private AangaraaPayResponse response;
} 