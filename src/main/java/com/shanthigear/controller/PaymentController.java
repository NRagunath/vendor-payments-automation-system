package com.shanthigear.controller;

import com.shanthigear.dto.PaymentResponseDTO;
import com.shanthigear.model.VendorPayment;
import com.shanthigear.model.Vendor;
import com.shanthigear.exception.DuplicateReferenceException;
import com.shanthigear.repository.VendorRepository;
import com.shanthigear.service.OracleHostToHostService;
import com.shanthigear.mapper.PaymentMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for handling payment-related endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@Validated
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final OracleHostToHostService paymentService;
    private final PaymentMapper paymentMapper;
    private final VendorRepository vendorRepository;

    @Autowired
    public PaymentController(OracleHostToHostService paymentService,
                           PaymentMapper paymentMapper, 
                           VendorRepository vendorRepository) {
        this.paymentService = paymentService;
        this.paymentMapper = paymentMapper;
        this.vendorRepository = vendorRepository;
    }

    /**
     * Create and process a new payment
     * @param payment The payment details
     * @return The processed payment with status
     */
    @PostMapping
    public ResponseEntity<?> createAndProcessPayment(@Valid @RequestBody VendorPayment payment) {
        try {
            logger.info("Processing new payment for vendor: {}", payment.getVendorId());
            
            // Validate payment amount
            if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                String error = "Payment amount must be greater than zero";
                logger.warn(error);
                return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("message", error));
            }
            
            if (payment.getVendorId() == null) {
                String error = "Vendor ID is required";
                logger.warn(error);
                return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("message", error));
            }
            
            // Process payment
            VendorPayment processedPayment = paymentService.processPayment(payment);
            logger.info("Successfully processed payment with reference: {}", processedPayment.getPaymentReference());
            return new ResponseEntity<>(processedPayment, HttpStatus.CREATED);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid payment data: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Collections.singletonMap("message", e.getMessage()));
        } catch (DuplicateReferenceException e) {
            logger.warn("Duplicate reference detected: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Collections.singletonMap("message", e.getMessage()));
        } catch (Exception e) {
            String errorMsg = "An error occurred while processing the payment: " + e.getMessage();
            logger.error(errorMsg, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonMap("message", errorMsg));
        }
    }

    /**
     * Get payment by reference number
     */
    @GetMapping("/{reference}")
    public ResponseEntity<?> getPayment(@PathVariable String reference) {
        try {
            logger.debug("Fetching payment with reference: {}", reference);
            if (!StringUtils.hasText(reference)) {
                return ResponseEntity.badRequest().body("Reference number cannot be empty");
            }
            
            return paymentService.getPaymentByReference(reference)
                    .map(payment -> {
                        logger.debug("Found payment with reference: {}", reference);
                        PaymentResponseDTO response = paymentMapper.toDto(payment);
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> {
                        logger.warn("Payment not found with reference: {}", reference);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            logger.error("Error fetching payment with reference {}: {}", reference, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body("An error occurred while fetching the payment: " + e.getMessage());
        }
    }

    /**
     * Get all payments for a vendor
     */
    @GetMapping("/vendors/{vendorId}")
    public ResponseEntity<?> getVendorPayments(
            @PathVariable String vendorId,
            @RequestParam(required = false) String status) {
        
        logger.info("Fetching payments for vendor: {}, status: {}", vendorId, status);
        
        try {
            // Validate vendor ID format (should be 'VEND' followed by numbers)
            if (!vendorId.matches("VEND\\d+")) {
                logger.warn("Invalid vendor ID format: {}", vendorId);
                return ResponseEntity.badRequest().body("Invalid vendor ID format. Vendor ID must be in format 'VEND' followed by numbers.");
            }
            // Look up vendor by the provided vendorId (vendorNumber)
            Optional<Vendor> vendor = vendorRepository.findByVendorNumber(vendorId);
            if (vendor.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Vendor not found with ID: " + vendorId);
            }
            
            List<VendorPayment> payments;
            if (status != null && !status.isEmpty()) {
                payments = paymentService.getVendorPaymentsByStatus(vendorId, status);
            } else {
                payments = paymentService.getPaymentsByVendor(vendorId);
            }
            
            logger.info("Found {} payments for vendor: {}", payments.size(), vendorId);
            return ResponseEntity.ok(payments);
            
        } catch (Exception e) {
            log.error("Error fetching payments for vendor: " + vendorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching payments: " + e.getMessage());
        }
    }

    /**
     * Get payments by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<VendorPayment>> getPaymentsByStatus(@PathVariable String status) {
        // This method is no longer supported as we're using vendor-specific endpoints
        // Consider removing or updating this endpoint based on your requirements
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Get overdue payments
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<VendorPayment>> getOverduePayments() {
        return ResponseEntity.ok(paymentService.getOverduePayments());
    }

    /**
     * Update payment status
     */
    @PutMapping("/{reference}/status")
    public ResponseEntity<?> updatePaymentStatus(
            @PathVariable String reference,
            @RequestParam String status,
            @RequestParam(required = false) String transactionId) {
        
        try {
            logger.info("Updating status for payment: {} to status: {}", reference, status);
            
            if (!StringUtils.hasText(reference)) {
                return ResponseEntity.badRequest().body("Reference number cannot be empty");
            }
            
            if (!StringUtils.hasText(status)) {
                return ResponseEntity.badRequest().body("Status cannot be empty");
            }
            
            VendorPayment updatedPayment = paymentService.updatePaymentStatus(reference, status, transactionId);
            logger.info("Successfully updated status for payment: {}", reference);
            return ResponseEntity.ok(updatedPayment);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid status update request for payment {}: {}", reference, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating payment status for reference {}: {}", reference, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body("An error occurred while updating the payment status: " + e.getMessage());
        }
    }

    /**
     * Search payments with filters
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchPayments(
            @RequestParam(required = false) String vendorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date toDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount) {
        
        try {
            if (vendorId != null) {
                // Validate vendor exists using the vendorNumber
                Optional<Vendor> vendor = vendorRepository.findByVendorNumber(vendorId);
                if (vendor.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Collections.singletonMap("message", "Vendor not found with ID: " + vendorId));
                }
                return ResponseEntity.ok(paymentService.getPaymentsByVendor(vendorId));
            } else if (status != null) {
                // Status-based search is not directly supported in the service layer
                // You may want to implement this in the service layer if needed
                return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                        .body("Status-based search is not implemented");
            } else {
                return ResponseEntity.badRequest().body("At least one search parameter is required");
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid vendor ID format");
        } catch (Exception e) {
            log.error("Error searching payments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error searching payments: " + e.getMessage());
        }
    }
}
