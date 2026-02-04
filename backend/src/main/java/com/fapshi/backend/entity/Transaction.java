package com.fapshi.backend.entity;

import com.fapshi.backend.enums.StatutTransaction;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal montant;

    private String description;

    private LocalDateTime dateHeure = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private StatutTransaction statut = StatutTransaction.PENDING;

    private String payToken;

    private String transactionIdExterne;

    private String phoneNumber;

    private String operateur;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne
    @JoinColumn(name = "vendeur_id")
    private Vendeur vendeur;

    // Relation inverse : pas de @JoinColumn ici !
    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private AangaraaPayRequest aangaraaPayRequest;
}