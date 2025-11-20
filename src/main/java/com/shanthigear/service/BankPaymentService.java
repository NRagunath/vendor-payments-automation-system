package com.shanthigear.service;

import com.shanthigear.payload.request.PaymentRequest;
import com.shanthigear.payload.response.PaymentResponse;

/**
 * Service interface for bank payment operations.
 */
public interface BankPaymentService {
    
    /**
     * Process a payment through the bank.
     *
     * @param paymentRequest the payment request details
     * @return the payment response
     */
    PaymentResponse processPayment(PaymentRequest paymentRequest);
    
    /**
     * Get the status of a payment.
     *
     * @param paymentId the payment ID
     * @return the payment status response
     */
    PaymentResponse getPaymentStatus(String paymentId);
}
