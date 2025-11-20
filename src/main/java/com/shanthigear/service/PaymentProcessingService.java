package com.shanthigear.service;

import com.shanthigear.exception.PaymentProcessingException;
import com.shanthigear.model.PaymentStatus;
import com.shanthigear.model.Vendor;
import com.shanthigear.model.VendorPayment;
import com.shanthigear.repository.VendorPaymentRepository;
import com.shanthigear.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for processing vendor payments through Oracle H2H.
 * Handles the complete payment lifecycle from invoice approval to bank transfer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessingService {

    private final VendorPaymentRepository vendorPaymentRepository;
    private final VendorRepository vendorRepository;
    private final OracleHostToHostService oracleHostToHostService;
    private final InvoiceService invoiceService;
    private final NotificationService notificationService;

    /**
     * Process a single payment for a vendor.
     * 
     * @param payment The payment to process
     * @return The processed payment with updated status
     * @throws PaymentProcessingException if there's an error processing the payment
     */
    /**
     * Process a single payment for a vendor.
     *
     * @param payment The payment to process
     * @return The processed payment with updated status
     * @throws PaymentProcessingException if there's an error processing the payment
     */
    @Transactional
    public VendorPayment processPayment(VendorPayment payment) throws PaymentProcessingException {
        log.info("Processing payment for vendor: {}", payment.getVendorId());
        
        try {
            // 1. Validate payment details
            validatePayment(payment);
            
            // 2. Get approved amount from invoice system
            BigDecimal approvedAmount = invoiceService.getApprovedAmount(payment.getInvoiceNumber());
            
            // 3. Verify the payment amount matches the approved amount
            if (payment.getAmount().compareTo(approvedAmount) != 0) {
                throw new PaymentProcessingException(
                    String.format("Payment amount %s does not match approved invoice amount %s", 
                    payment.getAmount(), approvedAmount));
            }
            
            // 4. Get vendor details
            Vendor vendor = vendorRepository.findByVendorNumber(payment.getVendorId())
                    .orElseThrow(() -> new PaymentProcessingException("Vendor not found with number: " + payment.getVendorId()));
            
            // 5. Update payment with vendor details
            updatePaymentWithVendorDetails(payment, vendor);
            
            // 6. Process payment through Oracle H2H
            VendorPayment processedPayment = oracleHostToHostService.processPayment(payment);
            
            // 7. Save the processed payment
            VendorPayment savedPayment = vendorPaymentRepository.save(processedPayment);
            
            // 8. Update invoice status and send notifications if payment is completed
            if (processedPayment.getStatus() == PaymentStatus.COMPLETED) {
                try {
                    invoiceService.markInvoiceAsPaid(
                        payment.getInvoiceNumber(), 
                        processedPayment.getPaymentReference());
                    
                    // Send payment confirmation asynchronously
                    sendPaymentConfirmationAsync(savedPayment);
                } catch (Exception e) {
                    log.error("Error in post-payment processing: {}", e.getMessage(), e);
                    // Don't fail the payment if post-processing fails
                }
            }
            
            return savedPayment;
            
        } catch (PaymentProcessingException e) {
            throw e; // Re-throw PaymentProcessingException as is
        } catch (Exception e) {
            String errorMsg = String.format("Failed to process payment for vendor %s: %s", 
                payment.getVendorId(), e.getMessage());
            log.error(errorMsg, e);
            
            payment.setStatus(PaymentStatus.FAILED);
            payment.setRemarks(errorMsg);
            vendorPaymentRepository.save(payment);
            
            throw new PaymentProcessingException(errorMsg, e);
        }
    }
    
    /**
     * Send payment confirmation asynchronously.
     * @param payment The payment to confirm
     */
    @Async
    protected void sendPaymentConfirmationAsync(VendorPayment payment) {
        try {
            notificationService.sendPaymentConfirmation(payment);
            log.info("Payment confirmation sent for payment: {}", payment.getPaymentReference());
        } catch (Exception e) {
            log.error("Failed to send payment confirmation for payment {}: {}", 
                payment.getPaymentReference(), e.getMessage(), e);
        }
    }
    
    /**
     * Process all approved invoices for payment.
     * This method can be scheduled to run periodically.
     */
    @Scheduled(cron = "0 0 9 * * ?") // Run daily at 9 AM
    @Transactional
    public void processApprovedInvoices() {
        log.info("Starting scheduled processing of approved invoices");
        
        try {
            List<VendorPayment> approvedInvoices = invoiceService.getApprovedInvoicesForPayment();
            log.info("Found {} approved invoices for processing", approvedInvoices.size());
            
            for (VendorPayment invoice : approvedInvoices) {
                try {
                    processPayment(invoice);
                    log.info("Successfully processed payment for invoice: {}", invoice.getInvoiceNumber());
                } catch (Exception e) {
                    log.error("Failed to process invoice {}: {}", 
                            invoice.getInvoiceNumber(), e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("Error in scheduled invoice processing: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Validates payment details.
     * @param payment The payment to validate
     * @throws IllegalArgumentException if payment is invalid
     */
    private void validatePayment(VendorPayment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment cannot be null");
        }
        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (payment.getInvoiceNumber() == null || payment.getInvoiceNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Invoice number is required");
        }
        if (payment.getVendorId() == null || payment.getVendorId().trim().isEmpty()) {
            throw new IllegalArgumentException("Vendor ID is required");
        }
    }
    
    /**
     * Updates payment with vendor details and sets initial status.
     * @param payment The payment to update
     * @param vendor The vendor details
     */
    private void updatePaymentWithVendorDetails(VendorPayment payment, Vendor vendor) {
        payment.setVendor(vendor);
        payment.setVendorName(vendor.getVendorName());
        payment.setVendorEmail(vendor.getEmailAddress());
        payment.setBankAccount(vendor.getBankAccountNum());
        payment.setIfscCode(vendor.getIfscCode());
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setCreatedAt(LocalDateTime.now());
    }
    
    /**
     * Process a batch of payments asynchronously.
     * @param payments List of payments to process
     * @return List of CompletableFuture for each payment processing
     */
    @Async
    public List<CompletableFuture<VendorPayment>> processBatchPaymentsAsync(List<VendorPayment> payments) {
        return payments.stream()
            .map(payment -> CompletableFuture.supplyAsync(() -> {
                try {
                    return processPayment(payment);
                } catch (Exception e) {
                    log.error("Error processing payment for vendor {}: {}", 
                        payment.getVendorId(), e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Check the status of a payment.
     * @param paymentReference The payment reference
     * @return The current payment status
     */
    @Transactional(readOnly = true)
    public PaymentStatus checkPaymentStatus(String paymentReference) {
        VendorPayment payment = vendorPaymentRepository.findByPaymentReference(paymentReference)
            .orElseThrow(() -> new PaymentProcessingException("Payment not found with reference: " + paymentReference));
            
        return payment.getStatus();
    }
    
    /**
     * Cancel a pending payment.
     * @param paymentReference The payment reference
     * @return true if cancellation was successful, false otherwise
     */
    @Transactional
    public boolean cancelPayment(String paymentReference) {
        VendorPayment payment = vendorPaymentRepository.findByPaymentReference(paymentReference)
            .orElseThrow(() -> new PaymentProcessingException("Payment not found with reference: " + paymentReference));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentProcessingException("Only pending payments can be cancelled");
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setRemarks("Payment cancelled by user");
        vendorPaymentRepository.save(payment);
        
        return true;
    }
}
