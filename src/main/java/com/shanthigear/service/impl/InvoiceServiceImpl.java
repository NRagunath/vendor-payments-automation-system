package com.shanthigear.service.impl;

import com.shanthigear.exception.InvoiceProcessingException;

import com.shanthigear.model.VendorPayment;
import com.shanthigear.repository.VendorPaymentRepository;
import com.shanthigear.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final VendorPaymentRepository paymentRepository;
    
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getApprovedAmount(String invoiceNumber) throws InvoiceProcessingException {
        log.info("Fetching approved amount for invoice: {}", invoiceNumber);
        
        // In a real implementation, this would call an external invoice system
        // For now, we'll simulate by checking existing payments
        return paymentRepository.findByReferenceNumber(invoiceNumber)
                .map(VendorPayment::getAmount)
                .orElseThrow(() -> new InvoiceProcessingException("Invoice not found or not approved: " + invoiceNumber));
    }
    
    @Override
    @Transactional
    public void markInvoiceAsPaid(String invoiceNumber, String paymentReference) throws InvoiceProcessingException {
        log.info("Marking invoice {} as paid with reference: {}", invoiceNumber, paymentReference);
        
        // In a real implementation, this would update the external invoice system
        VendorPayment payment = paymentRepository.findByReferenceNumber(invoiceNumber)
                .orElseThrow(() -> new InvoiceProcessingException("Invoice not found: " + invoiceNumber));
                
        payment.setPaymentReference(paymentReference);
        paymentRepository.save(payment);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<VendorPayment> getApprovedInvoicesForPayment() {
        log.info("Fetching all approved invoices ready for payment");
        
        // In a real implementation, this would query an external system for approved invoices
        // For now, return all pending payments
        return paymentRepository.findByStatus("APPROVED");
    }
}
