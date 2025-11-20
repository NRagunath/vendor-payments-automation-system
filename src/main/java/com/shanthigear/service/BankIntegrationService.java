package com.shanthigear.service;

import com.shanthigear.dto.PaymentRequestDTO;
import com.shanthigear.exception.BankApiException;
import com.shanthigear.model.BankTransaction;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for bank integration operations.
 * Handles communication with the bank's payment processing API.
 */
public interface BankIntegrationService {
    
    /**
     * Processes a payment through the bank's API.
     * 
     * @param paymentRequest the payment request details including amount, recipient, and reference
     * @return the payment reference number from the bank
     * @throws BankApiException if there's an error processing the payment
     */
    String processPayment(PaymentRequestDTO paymentRequest) throws BankApiException;
    
    /**
     * Verifies the status of a previously processed payment.
     * 
     * @param paymentReference the unique reference of the payment to verify
     * @return true if the payment was successfully processed and verified, false otherwise
     * @throws BankApiException if there's an error verifying the payment or if the payment reference is invalid
     * @throws IllegalArgumentException if the paymentReference is null or empty
     */
    boolean verifyPayment(String paymentReference) throws BankApiException;
    
    /**
     * Retrieves bank transactions for a given date range.
     * 
     * @param fromDate the start date (inclusive)
     * @param toDate the end date (inclusive)
     * @return list of bank transactions within the date range
     * @throws BankApiException if there's an error retrieving transactions
     */
    List<BankTransaction> getBankTransactions(LocalDate fromDate, LocalDate toDate) throws BankApiException;
}
