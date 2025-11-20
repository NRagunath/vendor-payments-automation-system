package com.shanthigear.service;

import com.shanthigear.exception.InvoiceProcessingException;
import com.shanthigear.model.VendorPayment;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for handling invoice processing and payment amount determination.
 */
public interface InvoiceService {
    
    /**
     * Get the approved payment amount for an invoice.
     * 
     * @param invoiceNumber The invoice number
     * @return The approved payment amount
     * @throws InvoiceProcessingException if the invoice is not found or not approved
     */
    BigDecimal getApprovedAmount(String invoiceNumber) throws InvoiceProcessingException;
    
    /**
     * Mark an invoice as paid after successful payment processing.
     * 
     * @param invoiceNumber The invoice number
     * @param paymentReference The payment reference
     * @throws InvoiceProcessingException if the invoice cannot be updated
     */
    void markInvoiceAsPaid(String invoiceNumber, String paymentReference) throws InvoiceProcessingException;
    
    /**
     * Get all approved invoices ready for payment.
     * 
     * @return List of approved invoices with payment details
     */
    List<VendorPayment> getApprovedInvoicesForPayment();
}
