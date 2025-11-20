package com.shanthigear.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a bank transaction record.
 */
@Data
public class BankTransaction {
    private String transactionId;
    private String referenceNumber;
    private LocalDate transactionDate;
    private BigDecimal amount;
    private String status;
    private String description;
    private String accountNumber;
    private String bankReference;
}
