package com.shanthigear.controller;

import com.shanthigear.service.OracleHostToHostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks/oracle")
@RequiredArgsConstructor
public class OracleWebhookController {

    private final OracleHostToHostService paymentService;

    @PostMapping("/payment-callback")
    public ResponseEntity<String> handlePaymentCallback(
            @RequestHeader("X-Oracle-Signature") String signature,
            @RequestHeader("X-Transaction-ID") String transactionId,
            @RequestParam("status") String status,
            @RequestParam(value = "message", required = false) String message) {
        
        log.info("Received payment callback for transaction: {} with status: {}", transactionId, status);
        
        try {
            // Verify the signature if needed
            // verifySignature(signature, transactionId, status, message);
            
            // Process the callback asynchronously
            paymentService.processPaymentCallback(transactionId, status, message);
            
            return ResponseEntity.ok("Callback received successfully");
        } catch (Exception e) {
            log.error("Error processing payment callback: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error processing callback: " + e.getMessage());
        }
    }
    
    // Add signature verification logic if needed
    /*
    private void verifySignature(String signature, String transactionId, String status, String message) {
        // Implement signature verification logic
        // Throw SecurityException if verification fails
    }
    */
}
