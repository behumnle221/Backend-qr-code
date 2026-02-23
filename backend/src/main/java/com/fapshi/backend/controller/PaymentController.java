package com.fapshi.backend.controller;

import com.fapshi.backend.dto.request.InitiatePaymentRequest;
import com.fapshi.backend.dto.response.PaymentInitResponse;
import com.fapshi.backend.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitResponse> initiatePayment(@RequestBody InitiatePaymentRequest request) {
        PaymentInitResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Endpoint de retour après paiement sur AangaraaPay
     * Ce endpoint est appelé par AangaraaPay après que le client a terminé le paiement
     */
    @GetMapping("/success")
    public ResponseEntity<Map<String, Object>> paymentSuccess(
            @RequestParam(required = false) String transaction_id,
            @RequestParam(required = false) String status) {
        
        System.out.println("📥 Retour AangaraaPay - transaction_id: " + transaction_id + ", status: " + status);
        
        // Retourner une page de succès simple
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Paiement traité avec succès",
            "transaction_id", transaction_id != null ? transaction_id : "unknown",
            "status", status != null ? status : "unknown"
        ));
    }
}