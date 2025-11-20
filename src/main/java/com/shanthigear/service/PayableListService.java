package com.shanthigear.service;

import com.shanthigear.model.PaymentDetails;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PayableListService {
    /**
     * Process payable list from uploaded file
     * @param file The uploaded file containing payable items
     * @param currentUser Username of the user uploading the file
     * @return List of processed payment details
     */
    List<PaymentDetails> processPayableList(MultipartFile file, String currentUser);
    
    /**
     * Get payment details by vendor number
     * @param vendorNumber The vendor number
     * @return List of payment details for the vendor
     */
    List<PaymentDetails> getPaymentDetailsByVendor(String vendorNumber);
    
    /**
     * Get all pending payments
     * @return List of pending payment details
     */
    List<PaymentDetails> getPendingPayments();
    
    /**
     * Update payment status
     * @param paymentReference The payment reference
     * @param newStatus The new status
     * @param updatedBy Username of the user updating the status
     * @return Updated payment details
     */
    PaymentDetails updatePaymentStatus(String paymentReference, String newStatus, String updatedBy);
}
