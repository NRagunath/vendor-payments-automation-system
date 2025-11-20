package com.shanthigear.service;

/**
 * Service for verifying payment statuses with retry capabilities.
 */
public interface PaymentVerificationService {
    
    /**
     * Verifies the status of a payment with the given reference number.
     * Implements retry logic for transient failures.
     *
     * @param paymentReference The payment reference number to verify
     * @return true if the payment is verified successfully, false otherwise
     */
    boolean verifyPayment(String paymentReference);
}
