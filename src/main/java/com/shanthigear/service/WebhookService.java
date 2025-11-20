package com.shanthigear.service;

import com.shanthigear.dto.WebhookPayload;
import com.shanthigear.model.PaymentStatus;
import com.shanthigear.model.VendorPayment;
import com.shanthigear.repository.VendorPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WebhookService {
    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    
    private final VendorPaymentRepository paymentRepository;
    private final EmailNotificationService emailNotificationService;

    @Transactional
    public void processPaymentWebhook(WebhookPayload payload) {
        try {
            logger.info("Processing webhook for payment: {}", payload.getPaymentId());
            
            // Find payment by payment reference or transaction ID (Java 8 compatible)
            Optional<VendorPayment> paymentOpt = paymentRepository.findByPaymentReference(payload.getPaymentId());
            if (!paymentOpt.isPresent() && payload.getTransactionId() != null) {
                paymentOpt = paymentRepository.findByTransactionId(payload.getTransactionId());
            }

            if (paymentOpt.isEmpty()) {
                logger.warn("No payment found for paymentId: {}", payload.getPaymentId());
                return;
            }

            VendorPayment payment = paymentOpt.get();
            
            // Process based on status
            switch (payload.getStatus().toUpperCase()) {
                case "SUCCESS":
                    handleSuccessfulPayment(payment, payload);
                    break;
                case "FAILED":
                    handleFailedPayment(payment, payload);
                    break;
                case "PENDING":
                    handlePendingPayment(payment, payload);
                    break;
                default:
                    logger.warn("Received unknown payment status: {}", payload.getStatus());
            }
            
        } catch (Exception e) {
            logger.error("Error processing payment webhook", e);
            throw new RuntimeException("Failed to process payment webhook", e);
        }
    }
    
    private void handleSuccessfulPayment(VendorPayment payment, WebhookPayload payload) {
        logger.info("Payment successful - ID: {}, Amount: {}", 
            payment.getPaymentReference(), 
            payment.getAmount()
        );
        
        // Update payment status
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId(payload.getTransactionId());
        
        // Extract UTR/Reference number from payload if available
        String utrNumber = payload.getUtrNumber();
        if (utrNumber != null && !utrNumber.trim().isEmpty()) {
            payment.setReferenceNumber(utrNumber);
        }
        
        String remarks = "Payment processed successfully and credited to vendor's account";
        if (utrNumber != null && !utrNumber.trim().isEmpty()) {
            remarks += ". UTR/Reference: " + utrNumber;
        }
        if (payload.getMetadata() != null) {
            remarks += ". " + payload.getMetadata().toString();
        }
        
        payment.setRemarks(remarks);
        payment = paymentRepository.save(payment);
        
        // Send payment credit notification only if payment is marked as COMPLETED
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            try {
                logger.info("Sending payment credit notification for payment ID: {}", payment.getPaymentReference());
                emailNotificationService.sendPaymentNotification(payment.getVendor(), payment);
                payment.setNotificationSent(true);
                paymentRepository.save(payment);
            } catch (Exception e) {
                logger.error("Failed to send payment credit notification email", e);
            }
        }
    }
    
    private void handleFailedPayment(VendorPayment payment, WebhookPayload payload) {
        logger.warn("Payment failed - ID: {}, Amount: {}", 
            payment.getPaymentReference(), 
            payment.getAmount()
        );
        
        // Update payment status
        payment.setStatus(PaymentStatus.FAILED);
        payment.setErrorDetails("Payment failed: " + 
            (payload.getMetadata() != null ? payload.getMetadata().toString() : ""));
        payment = paymentRepository.save(payment);
        
        // Send payment failure notification using the notification service
        try {
            String errorMessage = payload.getMetadata() != null ? 
                payload.getMetadata().toString() : "Payment processing failed";
            emailNotificationService.sendPaymentFailure(payment, errorMessage);
        } catch (Exception e) {
            logger.error("Failed to send payment failure notification", e);
        }
    }
    
    private void handlePendingPayment(VendorPayment payment, WebhookPayload payload) {
        logger.info("Payment pending - ID: {}, Amount: {}", 
            payment.getPaymentReference(), 
            payment.getAmount()
        );
        
        // Update payment status
        payment.setStatus(PaymentStatus.PENDING);
        payment.setRemarks("Payment is being processed. " + 
            (payload.getMetadata() != null ? payload.getMetadata().toString() : ""));
        paymentRepository.save(payment);
    }
}
