package com.shanthigear.controller;

import com.shanthigear.config.WebhookConfig;
import com.shanthigear.dto.WebhookPayload;
import com.shanthigear.util.WebhookSignatureVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanthigear.service.WebhookService;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.server.reactive.ServerHttpRequest;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {
    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    private static final String SIGNATURE_HEADER = "X-Bank-Signature";
    
    private final WebhookConfig webhookConfig;
    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    public WebhookController(WebhookConfig webhookConfig, WebhookService webhookService, ObjectMapper objectMapper) {
        this.webhookConfig = webhookConfig;
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/payment")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(SIGNATURE_HEADER) String signature,
            ServerHttpRequest request) {
        
        try {
            // Verify the webhook signature
            boolean isValid = WebhookSignatureVerifier.verifySignature(
                    payload, 
                    signature, 
                    webhookConfig.getSecret()
            );

            if (!isValid) {
                logger.warn("Invalid webhook signature received");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }

            // Parse the JSON payload
            WebhookPayload webhookPayload = objectMapper.readValue(payload, WebhookPayload.class);
            
            // Process the webhook payload
            logger.info("Processing webhook for payment: {}", webhookPayload.getPaymentId());
            webhookService.processPaymentWebhook(webhookPayload);
            
            return ResponseEntity.ok("Webhook processed successfully");
            
        } catch (Exception e) {
            logger.error("Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook");
        }
    }
}
