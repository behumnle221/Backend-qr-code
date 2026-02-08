package com.fapshi.backend.repository;

import com.fapshi.backend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByQrCodeVendeurId(Long vendeurId);
    List<Transaction> findByClientId(Long clientId);
}