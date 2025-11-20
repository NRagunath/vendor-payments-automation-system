package com.shanthigear.bank;

/**
 * Represents the status of a bank payment.
 */
public enum BankPaymentStatus {
    PENDING("PENDING", "Payment is being processed"),
    COMPLETED("COMPLETED", "Payment has been completed successfully"),
    FAILED("FAILED", "Payment has failed"),
    REJECTED("REJECTED", "Payment was rejected by the bank"),
    CANCELLED("CANCELLED", "Payment was cancelled");
    
    private final String code;
    private final String description;
    
    BankPaymentStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Converts a string status to a BankPaymentStatus enum value.
     * @param status The status string to convert
     * @return The corresponding BankPaymentStatus, or null if no match is found
     */
    public static BankPaymentStatus fromString(String status) {
        if (status == null) {
            return null;
        }
        
        for (BankPaymentStatus s : BankPaymentStatus.values()) {
            if (s.code.equalsIgnoreCase(status)) {
                return s;
            }
        }
        
        throw new IllegalArgumentException("No constant with code " + status + " found in BankPaymentStatus");
    }
}
