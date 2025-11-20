package com.shanthigear.service;

import com.shanthigear.exception.BatchProcessingException;
import com.shanthigear.exception.PaymentNotFoundException;
import com.shanthigear.exception.PaymentProcessingException;
import com.shanthigear.model.VendorPayment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for Oracle Host-to-Host (H2H) payment processing.
 * Handles the integration with Oracle's payment processing system.
 * This is the primary service for all payment-related operations.
 */
public interface OracleHostToHostService {

    /**
     * Processes a single payment through Oracle H2H.
     *
     * @param payment The payment to process
     * @return The processed payment with updated status
     * @throws PaymentProcessingException if there's an error processing the payment
     */
    VendorPayment processPayment(VendorPayment payment) throws PaymentProcessingException;

    /**
     * Processes multiple payments in a batch through Oracle H2H.
     *
     * @param payments List of payments to process
     * @return List of processed payments with updated statuses
     * @throws BatchProcessingException if there's an error processing the batch
     */
    List<VendorPayment> processBatchPayments(List<VendorPayment> payments) throws BatchProcessingException;

    /**
     * Checks the status of a payment in Oracle H2H.
     *
     * @param paymentReference The reference ID of the payment to check
     * @return The updated payment with current status
     * @throws PaymentNotFoundException if the payment is not found
     */
    VendorPayment checkPaymentStatus(String paymentReference) throws PaymentNotFoundException;

    /**
     * Validates if a payment can be processed through H2H.
     * Checks for required vendor and payment details.
     *
     * @param payment The payment to validate
     * @return Map of validation errors, empty if validation passes
     */
    Map<String, String> validatePaymentForH2H(VendorPayment payment);

    /**
     * Initiates the payment file generation and upload to the bank.
     *
     * @param paymentIds List of payment IDs to include in the upload
     * @param userId ID of the user initiating the upload
     * @return Upload result containing success status and reference ID
     */
    UploadResult initiateBankUpload(List<Long> paymentIds, String userId);

    /**
     * Verifies if the Oracle H2H service is available.
     *
     * @return true if the service is available and responding
     */
    boolean isServiceAvailable();

    /**
     * Gets the current status of a batch upload.
     *
     * @param batchId The ID of the batch upload
     * @return Batch status information
     */
    BatchStatus getBatchStatus(String batchId);

    /**
     * Retrieves payment history for a vendor within a date range.
     *
     * @param vendorId The ID of the vendor
     * @param startDate Start date of the range (inclusive)
     * @param endDate End date of the range (inclusive)
     * @return List of payments matching the criteria
     */
    List<VendorPayment> getVendorPaymentHistory(Long vendorId, LocalDate startDate, LocalDate endDate);

    /**
     * Calculates processing fees for a payment amount.
     *
     * @param amount The payment amount
     * @return The calculated processing fee
     */
    BigDecimal calculateProcessingFee(BigDecimal amount);

    /**
     * Reconciles payments between Oracle and bank records.
     *
     * @param fromDate Start date for reconciliation
     * @param toDate End date for reconciliation
     * @return Reconciliation result
     */
    ReconciliationResult reconcilePayments(LocalDate fromDate, LocalDate toDate);

    /**
     * Represents the result of a file upload operation.
     */
    interface UploadResult {
        boolean isSuccess();
        String getReferenceId();
        String getMessage();
        LocalDate getProcessedDate();
    }

    /**
     * Represents the status of a batch processing operation.
     */
    interface BatchStatus {
        String getBatchId();
        String getStatus();
        int getTotalPayments();
        int getProcessedPayments();
        int getFailedPayments();
        String getErrorMessage();
        LocalDate getCompletionDate();
    }

    /**
     * Represents the result of a reconciliation operation.
     */
    interface ReconciliationResult {
        LocalDate getFromDate();
        LocalDate getToDate();
        int getTotalRecords();
        int getMatchedRecords();
        int getUnmatchedRecords();
        List<ReconciliationMismatch> getMismatches();
    }

    /**
     * Represents a single reconciliation mismatch.
     */
    interface ReconciliationMismatch {
        String getPaymentReference();
        String getOracleStatus();
        String getBankStatus();
        String getDiscrepancyDetails();
    }

    // Methods from PaymentService
    
    /**
     * Get payment by reference number
     * @param reference The payment reference number
     * @return Optional containing the payment if found
     */
    Optional<VendorPayment> getPaymentByReference(String reference);
    
    /**
     * Get payments by vendor and status
     * @param vendorId The vendor ID
     * @param status The payment status
     * @return List of matching payments
     */
    List<VendorPayment> getVendorPaymentsByStatus(String vendorId, String status);
    
    /**
     * Process payment callback from Oracle H2H
     * @param transactionId The transaction ID from Oracle
     * @param status The status of the payment (e.g., COMPLETED, FAILED)
     * @param message Additional message or error details
     */
    void processPaymentCallback(String transactionId, String status, String message);
    
    /**
     * Update payment status with transaction details
     * @param reference The payment reference
     * @param status The new status
     * @param transactionId The transaction ID
     * @return The updated payment
     */
    VendorPayment updatePaymentStatus(String reference, String status, String transactionId);
    
    /**
     * Get all payments for a vendor
     * @param vendorId The vendor ID
     * @return List of vendor's payments
     */
    List<VendorPayment> getPaymentsByVendor(String vendorId);
    
    /**
     * Get overdue payments
     * @return List of overdue payments
     */
    List<VendorPayment> getOverduePayments();
    
    /**
     * Save payment record
     * @param payment The payment to save
     * @return The saved payment
     */
    VendorPayment save(VendorPayment payment);
    
    
    /**
     * Verify if a payment exists with the given reference
     * @param paymentReference The payment reference to check
     * @return true if payment exists and is verified
     */
    boolean verifyPayment(String paymentReference);
}
