package com.fapshi.backend.controller;

import com.fapshi.backend.dto.request.InitiatePaymentRequest;
import com.fapshi.backend.dto.request.VirtualPaymentRequest;
import com.fapshi.backend.dto.response.PaymentInitResponse;
import com.fapshi.backend.entity.Client;
import com.fapshi.backend.entity.Transaction;
import com.fapshi.backend.repository.TransactionRepository;
import com.fapshi.backend.service.ClientService;
import com.fapshi.backend.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private ClientService clientService;

    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitResponse> initiatePayment(@RequestBody InitiatePaymentRequest request) {
        PaymentInitResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Paiement simplif\u00e9 par solde virtuel interne
     * Requ\u00eate minimale: uniquement qrCodeId et montant
     */
    @PostMapping("/virtual")
    public ResponseEntity<PaymentInitResponse> initiateVirtualPayment(
            Authentication authentication,
            @RequestBody VirtualPaymentRequest request) {
        
        // R\u00e9cup\u00e9rer le client depuis l'authentification (JWT)
        String username = authentication.getName();
        Client client = clientService.findByEmail(username)
                .or(() -> clientService.findByTelephone(username))
                .orElseThrow(() -> new RuntimeException("Client non trouv\u00e9"));
        
        // Appel au service avec l'ID du client
        PaymentInitResponse response = paymentService.initiateVirtualPayment(client.getId(), request);
        return ResponseEntity.ok(response);
    }
    /**
     * Endpoint de retour après paiement sur AangaraaPay
     * Ce endpoint est appelé par AangaraaPay après que le client a terminé le paiement
     * 
     * Note: Le vrai traitement se fait via le webhook /api/webhook/aangaraa
     * Cet endpoint sert de page de redirection pour le client
     */
    @GetMapping("/success")
    public ResponseEntity<Map<String, Object>> paymentSuccess(
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String transaction_id,
            @RequestParam(required = false) String status) {
        
        System.out.println("📥 Retour AangaraaPay - token: " + token + ", transaction_id: " + transaction_id + ", status: " + status);
        
        // Essayer de trouver la transaction par token ou transaction_id
        Transaction transaction = null;
        
        if (token != null && !token.isEmpty()) {
            Optional<Transaction> optTrans = transactionRepository.findByPayToken(token);
            if (optTrans.isPresent()) {
                transaction = optTrans.get();
            }
        }
        
        if (transaction == null && transaction_id != null && !transaction_id.isEmpty()) {
            Optional<Transaction> optTrans = transactionRepository.findByTransactionId(transaction_id);
            if (optTrans.isPresent()) {
                transaction = optTrans.get();
            } else {
                // Essayer par ID
                try {
                    Long tId = Long.parseLong(transaction_id);
                    Optional<Transaction> optById = transactionRepository.findById(tId);
                    if (optById.isPresent()) {
                        transaction = optById.get();
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }
        
        if (transaction != null) {
            String dbStatus = transaction.getStatut();
            System.out.println("✅ Transaction trouvée - ID: " + transaction.getId() + ", Statut DB: " + dbStatus);
            
            return ResponseEntity.ok(Map.of(
                "success", "SUCCESS".equals(dbStatus) || "SUCCESSFUL".equals(dbStatus),
                "message", "SUCCESS".equals(dbStatus) ? "Paiement réussi" : "Paiement " + dbStatus,
                "transaction_id", transaction.getId(),
                "status", dbStatus != null ? dbStatus : "unknown",
                "amount", transaction.getMontant() != null ? transaction.getMontant().toString() : "0",
                "payToken", transaction.getPayToken() != null ? transaction.getPayToken() : ""
            ));
        }
        
        // Transaction non trouvée - utiliser les paramètres fournis
        System.out.println("⚠️ Transaction non trouvée en base");
        return ResponseEntity.ok(Map.of(
            "success", false,
            "message", "Transaction non trouvée",
            "transaction_id", transaction_id != null ? transaction_id : "unknown",
            "status", status != null ? status : "unknown"
        ));
    }
    
    /**
     * Alias pour /success - supporte les deux chemins (/payment/success et /payments/success)
     */
    @GetMapping("/payment/success")
    public ResponseEntity<Map<String, Object>> paymentSuccessAlias(
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String transaction_id,
            @RequestParam(required = false) String status) {
        return paymentSuccess(token, transaction_id, status);
    }
    
    /**
     * Vérifie le statut du paiement directement auprès d'Aangaraa
     */
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<Map<String, Object>> checkPaymentStatus(@PathVariable Long transactionId) {
        try {
            Map<String, Object> status = paymentService.checkPaymentStatus(transactionId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Vérifie le statut local de la transaction (sans appeler Aangaraa)
     */
    @GetMapping("/status/local/{transactionId}")
    public ResponseEntity<Map<String, Object>> getLocalStatus(@PathVariable Long transactionId) {
        Optional<Transaction> optTrans = transactionRepository.findById(transactionId);
        if (optTrans.isPresent()) {
            Transaction t = optTrans.get();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "transactionId", t.getId(),
                "status", t.getStatut(),
                "montant", t.getMontant(),
                "payToken", t.getPayToken() != null ? t.getPayToken() : ""
            ));
        }
        return ResponseEntity.notFound().build();
    }
}

