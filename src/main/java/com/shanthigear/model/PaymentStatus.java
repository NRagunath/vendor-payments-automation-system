package com.shanthigear.model;

/**
 * Represents the status of a payment transaction.
 */
public enum PaymentStatus {
    PENDING,                // Payment has been initiated but not yet processed
    PROCESSING,             // Payment is being processed by the bank
    PENDING_VERIFICATION,   // Payment is pending verification from the bank
    APPROVED,               // Payment has been approved by the bank
    COMPLETED,              // Payment was successfully processed and verified
    RECONCILED,             // Payment has been reconciled with bank statement
    FAILED,                 // Payment processing failed
    CANCELLED,              // Payment was cancelled
    REFUNDED,               // Payment was refunded
    DECLINED,               // Payment was declined by the bank
    REVERSED;               // Payment was reversed
    
    /**
     * Converts a string status to the corresponding enum value.
     * Returns PENDING if the string doesn't match any status.
     * 
     * @param statusStr the status string to convert
     * @return the corresponding PaymentStatus, or PENDING if not found
     */
    public static PaymentStatus fromStatusString(String statusStr) {
        if (statusStr == null) {
            return PENDING;
        }
        try {
            return valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PENDING;
        }
    }
}
