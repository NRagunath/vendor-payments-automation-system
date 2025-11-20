package com.shanthigear.model;

/**
 * Represents the status of a Host-to-Host (H2H) payment processing.
 */
public enum H2HStatus {
    /**
     * Payment is pending processing.
     */
    PENDING,
    
    /**
     * Payment has been successfully processed by the bank.
     */
    PROCESSED,
    
    /**
     * Payment processing failed.
     */
    FAILED,
    
    /**
     * Payment has been reversed.
     */
    REVERSED,
    
    /**
     * Payment is in progress at the bank.
     */
    IN_PROGRESS,
    
    /**
     * Payment has been rejected by the bank.
     */
    REJECTED
}
