package com.fapshi.backend.controller;

import com.fapshi.backend.dto.response.TransactionDTO;
import com.fapshi.backend.dto.response.TransactionListResponse;
import com.fapshi.backend.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/client")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @GetMapping("/transactions")
    public ResponseEntity<TransactionListResponse> getTransactions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String dateDebut,
            @RequestParam(required = false) String dateFin) {

        Long clientId = (Long) authentication.getCredentials();  // Ajuste selon ton JWT (ex : userId du client)

        // Appel au service pour récupérer les transactions paginées
        List<TransactionDTO> transactions = clientService.getHistoriqueTransactions(clientId, page, size, statut, dateDebut, dateFin);

        // Création de la réponse paginée (adaptée si tu as une Page, sinon ajuste)
        TransactionListResponse response = new TransactionListResponse();
        response.setContent(transactions);
        response.setTotalElements(transactions.size());  // Ajuste avec le vrai total si pagination réelle
        response.setTotalPages(1);  // Ajuste avec le vrai nombre de pages
        response.setCurrentPage(page);
        response.setPageSize(size);
        response.setHasNextPage(false);  // Ajuste selon pagination
        response.setHasPreviousPage(page > 0);

        return ResponseEntity.ok(response);
    }
}