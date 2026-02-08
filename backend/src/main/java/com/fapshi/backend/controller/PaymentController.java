package com.fapshi.backend.controller;

import com.fapshi.backend.dto.request.InitiatePaymentRequest;
import com.fapshi.backend.dto.response.PaymentInitResponse;
import com.fapshi.backend.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}