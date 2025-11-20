package com.shanthigear.model;

import lombok.Data;

import java.time.LocalDate;

/**
 * Represents an action item assigned to internal teams.
 */
@Data
public class ActionItem {
    private String id;
    private String title;
    private String description;
    private String assignedTo;
    private String assignedBy;
    private String status;       // OPEN, IN_PROGRESS, COMPLETED, CANCELLED
    private String priority;     // HIGH, MEDIUM, LOW
    private LocalDate dueDate;
    private LocalDate completedDate;
    private String relatedPaymentId;
    private String relatedVendorId;
    private String category;     // e.g., "VALIDATION", "BANK_UPLOAD", "NOTIFICATION"
    private String comments;
}
