package com.shanthigear.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Represents a summary of payments for internal reporting.
 */
@Data
public class PaymentSummary {
    private String batchId;
    private LocalDate paymentDate;
    private int totalPayments;
    private BigDecimal totalAmount;
    private String currency;
    private Map<String, Integer> statusCounts;
    private Map<String, BigDecimal> currencyBreakdown;
    private List<PaymentDetail> paymentDetails;
    private String generatedBy;
    private LocalDate generatedAt;
    private String processingTime;
    
    @Data
    public static class PaymentDetail {
        private String paymentReference;
        private String vendorName;
        private String vendorId;
        private BigDecimal amount;
        private String status;
        private String bankAccount;
        private String ifscCode;
        private String utrNumber;
    }
}
