package com.shanthigear.controller;

import com.shanthigear.payload.request.PaymentRequest;
import com.shanthigear.payload.response.PaymentResponse;
import com.shanthigear.service.BankPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * REST controller for handling bank payment operations.
 */
@RestController
@RequestMapping("/api/bank/payments")
public class BankPaymentController {

    private final BankPaymentService bankPaymentService;

    @Autowired
    public BankPaymentController(BankPaymentService bankPaymentService) {
        this.bankPaymentService = bankPaymentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('BANK_API')")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest paymentRequest) {
        PaymentResponse response = bankPaymentService.processPayment(paymentRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasRole('BANK_API')")
    public ResponseEntity<PaymentResponse> getPaymentStatus(@PathVariable String paymentId) {
        PaymentResponse response = bankPaymentService.getPaymentStatus(paymentId);
        return ResponseEntity.ok(response);
    }
}
