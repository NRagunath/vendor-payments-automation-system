package com.shanthigear.service;

import com.shanthigear.model.*;

import java.util.List;

/**
 * Interface for sending payment-related notifications.
 */
public interface NotificationService {
    // Vendor notifications
    /**
     * Sends a payment notification to the vendor.
     * @param vendor The vendor to notify
     * @param payment The payment details to include in the notification
     */
    void sendPaymentNotification(Vendor vendor, VendorPayment payment);
    
    /**
     * Sends a payment confirmation notification.
     * @param payment The payment details to include in the notification
     */
    void sendPaymentConfirmation(VendorPayment payment);
    
    /**
     * Sends a payment failure notification.
     * @param payment The payment details that failed
     * @param reason The reason for the payment failure
     */
    void sendPaymentFailure(VendorPayment payment, String reason);
    
    /**
     * Sends an overdue payment notification.
     * @param payment The payment that is overdue
     */
    void sendOverduePaymentNotification(VendorPayment payment);
    
    // Internal team notifications
    /**
     * Sends a payment summary to internal teams.
     * @param summary The payment summary data
     * @param recipients List of email addresses to receive the summary
     */
    void sendPaymentSummary(PaymentSummary summary, List<String> recipients);
    
    /**
     * Sends an exception report to internal teams.
     * @param exceptions List of payment exceptions
     * @param recipients List of email addresses to receive the report
     */
    void sendExceptionReport(List<PaymentException> exceptions, List<String> recipients);
    
    /**
     * Sends action items to responsible team members.
     * @param actionItems List of action items
     * @param recipients List of email addresses to receive the action items
     */
    void sendActionItems(List<ActionItem> actionItems, List<String> recipients);
    
    /**
     * Sends a daily summary report to internal teams.
     * @param summary The daily summary data
     * @param recipients List of email addresses to receive the daily summary
     */
    void sendDailySummary(PaymentSummary summary, List<String> recipients);
}
