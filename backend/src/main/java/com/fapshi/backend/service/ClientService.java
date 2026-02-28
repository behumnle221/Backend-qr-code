package com.fapshi.backend.service;

import com.fapshi.backend.entity.Client;
import com.fapshi.backend.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// AJOUT : Import pour les DTOs et autres dépendances nécessaires
import com.fapshi.backend.dto.response.TransactionDTO;  // ← Changé de TransactionResponse à TransactionDTO
import com.fapshi.backend.entity.Transaction;
import com.fapshi.backend.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.stream.Collectors;

/**
 * Service pour les opérations spécifiques aux Clients.
 */
@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    // AJOUT : Injection du TransactionRepository pour gérer les transactions
    @Autowired
    private TransactionRepository transactionRepository;

    public Client save(Client client) {
        return clientRepository.save(client);
    }

    public Optional<Client> findById(Long id) {
        return clientRepository.findById(id);
    }

    public List<Client> findAll() {
        return clientRepository.findAll();
    }

    // AJOUT : Nouvelle méthode pour récupérer l'historique des transactions d'un client
    // (basée sur le clientId ou telephoneClient, ajusté à ton entité Transaction)
    public List<TransactionDTO> getHistoriqueTransactions(Long clientId, int page, int size, String statut, String dateDebut, String dateFin) {  // ← Changé TransactionResponse à TransactionDTO
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreation").descending());

        // Récupération des transactions du client (ajuste si tu utilises telephoneClient au lieu de clientId)
        Page<Transaction> transactions = transactionRepository.findTransactionsByClient(clientId, statut, LocalDateTime.parse(dateDebut), LocalDateTime.parse(dateFin), pageable);  // Appel à la nouvelle méthode

        // AJOUT : Filtres optionnels (statut, dates) - tu peux les ajouter ici si besoin
        // Pour l'instant, c'est basique ; on peut raffiner plus tard

        return transactions.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // AJOUT : Méthode privée pour convertir Transaction en DTO Response
    private TransactionDTO toResponse(Transaction transaction) {  // ← Changé TransactionResponse à TransactionDTO
        TransactionDTO response = new TransactionDTO();
        response.setId(transaction.getId());
        response.setMontant(transaction.getMontant());
        response.setStatut(transaction.getStatut());
        response.setDateCreation(transaction.getDateCreation());
        // AJOUT : Ajoute d'autres champs si besoin (ex : operator, payToken)
        return response;
    }
}