package com.shanthigear.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents a mismatch found during payment reconciliation.
 */
@Data
@AllArgsConstructor
public class ReconciliationMismatch {
    private String paymentReference;
    private String fieldName;
    private Object expectedValue;
    private Object actualValue;
    private String description;
}
