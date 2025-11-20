package com.shanthigear.service;

import com.shanthigear.model.PaymentStatus;
import com.shanthigear.model.Vendor;
import com.shanthigear.model.VendorPayment;
import com.shanthigear.repository.VendorPaymentRepository;
import com.shanthigear.repository.VendorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.shanthigear.exception.ResourceNotFoundException;
import com.shanthigear.exception.PaymentProcessingException;

/**
 * Service for managing vendor payments and processing payment workflows.
 * Handles the complete payment processing lifecycle including validation,
 * payment execution, and notification.
 */
@Service
public class VendorPaymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(VendorPaymentProcessor.class);
    
    private final VendorRepository vendorRepository;
    private final VendorPaymentRepository paymentRepository;
    private final OracleHostToHostService oracleHostToHostService;
    private final VendorService vendorService;

    public VendorPaymentProcessor(VendorRepository vendorRepository,
                               VendorPaymentRepository paymentRepository,
                               OracleHostToHostService oracleHostToHostService,
                               VendorService vendorService) {
        this.vendorRepository = vendorRepository;
        this.paymentRepository = paymentRepository;
        this.oracleHostToHostService = oracleHostToHostService;
        this.vendorService = vendorService;
    }

    /**
     * Process a payment for a vendor.
     *
     * @param vendorId The ID of the vendor
     * @param amount The payment amount
     * @param invoiceNumber The invoice number
     * @param description Payment description
     * @return The processed payment
     */
    @Transactional
    public VendorPayment processVendorPayment(String vendorNumber, 
                                            BigDecimal amount, 
                                            String invoiceNumber,
                                            String description) {
        // Find the vendor by vendor number
        Vendor vendor = vendorRepository.findByVendorNumber(vendorNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with number: " + vendorNumber));
            
        // Check if vendor is eligible for H2H based on required fields
        if (!vendorService.isEligibleForH2H(vendorNumber)) {
            String errorMsg = String.format(
                "Vendor with number %s is not eligible for Host to Host payments. " +
                "Required fields: bank account number, IFSC code, bank name, email address, and pay group.", 
                vendorNumber);
            logger.warn("Payment rejected for vendor {}: {}", vendorNumber, errorMsg);
            throw new PaymentProcessingException(errorMsg);
        }
        
        // Create a new payment record
        VendorPayment payment = createPaymentRecord(vendor, amount, invoiceNumber, description);
        
        try {
            // Process payment through Oracle H2H
            VendorPayment processedPayment = oracleHostToHostService.processPayment(payment);
            
            // Update payment with any changes from the processing
            payment.setStatus(processedPayment.getStatus());
            payment.setTransactionId(processedPayment.getTransactionId());
            
            // Update payment status based on result
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                completePayment(payment);
                logger.info("Payment completed successfully for vendor: {}, reference: {}", 
                           vendorNumber, payment.getPaymentReference());
            } else {
                String errorMsg = "Payment processing failed: " + 
                    (payment.getErrorDetails() != null ? payment.getErrorDetails() : "Unknown error");
                failPayment(payment, errorMsg);
                logger.error("Payment failed for vendor: {}, reference: {}, reason: {}", 
                           vendorNumber, payment.getPaymentReference(), errorMsg);
            }
            
            return payment;
        } catch (Exception e) {
            failPayment(payment, "Error processing payment: " + e.getMessage());
            logger.error("Error processing payment for vendor: {}, reference: {}", 
                        vendorNumber, payment.getPaymentReference(), e);
            throw new RuntimeException("Failed to process payment: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create a new payment record in the database.
     */
    private VendorPayment createPaymentRecord(Vendor vendor, BigDecimal amount, 
                                             String invoiceNumber, String description) {
        VendorPayment payment = new VendorPayment();
        payment.setVendor(vendor);  // Set the vendor relationship
        payment.setVendorName(vendor.getVendorName());
        payment.setVendorEmail(vendor.getEmailAddress());
        payment.setAmount(amount);
        payment.setInvoiceNumber(invoiceNumber);
        payment.setPaymentReference(UUID.randomUUID().toString());
        payment.setStatus(PaymentStatus.PENDING.name());
        payment.setPaymentDate(LocalDate.now());
        payment.setBankAccount(vendor.getBankAccountNum());
        payment.setIfscCode(vendor.getIfscCode());
        payment.setDescription(description);
        
        return paymentRepository.save(payment);
    }
    
    /**
     * Mark a payment as completed and send notification.
     */
    private void completePayment(VendorPayment payment) {
        payment.setStatus(PaymentStatus.COMPLETED.name());
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        
        // Send payment confirmation asynchronously
        sendPaymentConfirmation(payment);
    }
    
    /**
     * Mark a payment as failed.
     */
    private void failPayment(VendorPayment payment, String error) {
        payment.setStatus(PaymentStatus.FAILED.name());
        payment.setErrorDetails(error);
        paymentRepository.save(payment);
    }
    
    /**
     * Get payment by ID.
     */
    public Optional<VendorPayment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }
    
    /**
     * Get all payments for a vendor.
     * @param vendorId the vendor ID as a String
     * @return list of payments for the vendor, ordered by creation date (newest first)
     */
    public List<VendorPayment> getPaymentsByVendor(String vendorNumber) {
        // First find the vendor by vendor number
        Vendor vendor = vendorRepository.findByVendorNumber(vendorNumber)
            .orElseThrow(() -> new IllegalArgumentException("Vendor not found with number: " + vendorNumber));
        // Then find payments by vendor ID
        return paymentRepository.findByVendorIdOrderByCreatedAtDesc(vendor.getVendorNumber());
    }
    
    /**
     * Get payment by reference number.
     */
    @Transactional(readOnly = true)
    public Optional<VendorPayment> getPaymentByReference(String reference) {
        return paymentRepository.findByPaymentReference(reference);
    }
    
    /**
     * Complete a payment with IFSC code and update payment details
     * 
     * @param vendor The vendor associated with the payment
     * @param payment The payment to be completed
     * @param description Payment description
     * @param error Error details if any, can be null
     * @return List of payments for the vendor, ordered by creation date (newest first)
     */
    @Transactional
    public List<VendorPayment> completeVendorPayment(Vendor vendor, VendorPayment payment, 
                                                   String description, String error) {
        // Set IFSC code from vendor to payment
        payment.setIfscCode(vendor.getIfscCode());
        
        // Set the payment description
        payment.setDescription(description);
        
        // Set the completion timestamp
        payment.setCompletedAt(LocalDateTime.now());
        
        // Set error details if provided
        if (error != null) {
            payment.setErrorDetails(error);
        }
        
        // Save the updated payment
        paymentRepository.save(payment);
        
        // Return the list of payments for the vendor, ordered by creation date (newest first)
        return paymentRepository.findByVendorIdOrderByCreatedAtDesc(vendor.getVendorNumber());
    }
    
    /**
     * Send payment confirmation asynchronously.
     */
    @Async
    public void sendPaymentConfirmation(VendorPayment payment) {
        try {
            // In a real application, this would send an email or notification
            // For now, we'll just log it
            logger.info("Sending payment confirmation for payment: {}", payment.getPaymentReference());
            
            // Simulate async processing
            Thread.sleep(1000);
            
            logger.info("Payment confirmation sent for payment: {}", payment.getPaymentReference());
        } catch (Exception e) {
            logger.error("Error sending payment confirmation for payment {}: {}", 
                       payment.getPaymentReference(), e.getMessage(), e);
        }
    }
}
