package com.shanthigear.model;

/**
 * Represents the status of a payment batch.
 */
public enum BatchStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    RECONCILED,
    RECONCILIATION_FAILED,
    APPROVED,
    REJECTED
}
