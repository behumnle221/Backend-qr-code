package com.fapshi.backend.service;

import com.fapshi.backend.entity.Transaction;
import com.fapshi.backend.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service pour gérer les transactions (paiement, retrait, historique).
 */
@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }

    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    // Historique des transactions d'un client
    public List<Transaction> findByClientId(Long clientId) {
        return transactionRepository.findByClientId(clientId);
    }

    // Transactions reçues par un vendeur
    // public List<Transaction> findByVendeurId(Long vendeurId) {
    //     return transactionRepository.findByVendeurId(vendeurId);
    // }
    // Transactions reçues par un vendeur
public List<Transaction> findByVendeurId(Long vendeurId) {
    // On appelle la méthode telle qu'elle est définie dans le Repository
    return transactionRepository.findByQrCodeVendeurId(vendeurId);
}
}