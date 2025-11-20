package com.shanthigear.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Represents a payment exception that needs to be reported to internal teams.
 */
@Data
public class PaymentException {
    private String paymentReference;
    private String vendorName;
    private String vendorId;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime timestamp;
    private String severity;  // CRITICAL, HIGH, MEDIUM, LOW
    private String module;    // e.g., "VALIDATION", "BANK_UPLOAD", "NOTIFICATION"
    private String suggestedAction;
    private String assignedTo;
    private String status;    // e.g., "OPEN", "IN_PROGRESS", "RESOLVED"
    private boolean reported; // Whether the exception has been reported
    
    // Additional getters and setters for the new fields
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public boolean isReported() {
        return reported;
    }
    
    public void setReported(boolean reported) {
        this.reported = reported;
    }
}
