package com.shanthigear.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Represents a vendor payment with all relevant details.
 * This includes both payment information and bank integration details.
 */
@Entity
@Table(name = "VENDOR_PAYMENT")
@SequenceGenerator(name = "vendor_payment_seq", sequenceName = "VENDOR_PAYMENT_SEQ", allocationSize = 1)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class VendorPayment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vendor_payment_seq")
    @Column(name = "ID")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VENDOR_ID_FK", nullable = false, referencedColumnName = "ID")
    private Vendor vendor;
    
    @Column(name = "VENDOR_ID", insertable = false, updatable = false, length = 20)
    private String vendorId; // This is a read-only field that mirrors vendor.vendorId
    
    @Size(max = 255, message = "Vendor name must be less than 255 characters")
    @Column(name = "VENDOR_NAME", nullable = false, length = 255)
    private String vendorName;
    
    @Email(message = "Vendor email should be valid")
    @Size(max = 100, message = "Vendor email must be less than 100 characters")
    @Column(name = "VENDOR_EMAIL", length = 100)
    private String vendorEmail;
    
    @Size(max = 50, message = "Bank account must be less than 50 characters")
    @Column(name = "BANK_ACCOUNT", length = 50)
    private String bankAccount;
    
    @Size(max = 20, message = "IFSC code must be less than 20 characters")
    @Column(name = "IFSC_CODE", length = 20)
    private String ifscCode;
    
    @Size(max = 255, message = "Custom field 1 must be less than 255 characters")
    @Column(name = "CUSTOM_FIELD1", length = 255)
    private String customField1;
    
    @Size(max = 50, message = "Reference number must be less than 50 characters")
    @Column(name = "REFERENCE_NUMBER", length = 50)
    private String referenceNumber;  // UTR/Reference number for the transaction
    
    @Size(max = 50, message = "Invoice number must be less than 50 characters")
    @Column(name = "INVOICE_NUMBER", length = 50)
    private String invoiceNumber;
    
    @Column(name = "AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "REVERSED_PAYMENT_ID")
    private Long reversedPaymentId;
    
    @Size(max = 50, message = "Payment reference must be less than 50 characters")
    @Column(name = "PAYMENT_REFERENCE", nullable = false, unique = true, length = 50)
    private String paymentReference;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private PaymentStatus status;
    
    /**
     * Helper method to safely convert a string to PaymentStatus.
     * @param statusStr the status string to convert
     * @return the corresponding PaymentStatus, or PENDING if invalid
     */
    public static PaymentStatus fromStatusString(String statusStr) {
        if (statusStr == null) {
            return PaymentStatus.PENDING;
        }
        try {
            return PaymentStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PaymentStatus.PENDING;
        }
    }
    
    @Size(max = 100, message = "Transaction ID must be less than 100 characters")
    @Column(name = "TRANSACTION_ID", length = 100)
    private String transactionId;
    
    @Column(name = "PAYMENT_DATE")
    private LocalDate paymentDate;
    
    @Size(max = 50, message = "Bank reference must be less than 50 characters")
    @Column(name = "BANK_REFERENCE", length = 50)
    private String bankReference;
    
    @Lob
    @Column(name = "REMARKS")
    private String remarks;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BATCH_PAYMENT_ID", referencedColumnName = "ID")
    private BatchPayment batchPayment;
    
    @Column(name = "H2H_PROCESSED", nullable = false)
    @Builder.Default
    private boolean h2hProcessed = false;
    
    @Column(name = "H2H_PROCESSED_AT")
    private LocalDateTime h2hProcessedAt;
    
    @Size(max = 100, message = "H2H reference must be less than 100 characters")
    @Column(name = "H2H_REFERENCE", length = 100)
    private String h2hReference;
    
    @Size(max = 100, message = "Export reference must be less than 100 characters")
    @Column(name = "EXPORT_REFERENCE", length = 100)
    private String exportReference;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "H2H_STATUS", length = 20)
    @Builder.Default
    private H2HStatus h2hStatus = H2HStatus.PENDING;
    
    @Lob
    @Column(name = "H2H_ERROR_MESSAGE")
    private String h2hErrorMessage;
    
    @Column(name = "IS_VOID", nullable = false)
    @Builder.Default
    private boolean voided = false;
    
    @Size(max = 1000, message = "Deletion reason must be less than 1000 characters")
    @Column(name = "DELETION_REASON", length = 1000)
    private String deletionReason;
    
    @Column(name = "RECONCILED_AT")
    private LocalDateTime reconciledAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DELETED_BY", referencedColumnName = "ID")
    private User deletedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VOIDED_BY", referencedColumnName = "ID")
    private User voidedBy;
    
    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt;
    
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
    
    @Column(name = "IS_NOTIFICATION_SENT", nullable = false)
    @Builder.Default
    private boolean notificationSent = false;
    
    @Size(max = 4000, message = "Notes must be less than 4000 characters")
    @Column(name = "NOTES", length = 4000)
    private String notes;
    
    @Size(max = 50, message = "Batch ID must be less than 50 characters")
    @Column(name = "BATCH_ID", length = 50)
    private String batchId;
    
    @Size(max = 50, message = "Created by must be less than 50 characters")
    @Column(name = "CREATED_BY", length = 50)
    private String createdBy;
    
    @Size(max = 3, message = "Payment currency must be 3 characters (ISO 4217)")
    @Column(name = "PAYMENT_CURRENCY", length = 3)
    private String paymentCurrency;
    
    @Size(max = 3, message = "Currency code must be 3 characters (ISO 4217)")
    @Column(name = "CURRENCY", length = 3)
    private String currency;
    
    @Column(name = "IS_RECONCILED")
    private boolean reconciled;
    
    @Column(name = "RECONCILIATION_DATE")
    private LocalDateTime reconciliationDate;
    
    @Size(max = 100, message = "Reconciliation reference must be less than 100 characters")
    @Column(name = "RECONCILIATION_REFERENCE", length = 100)
    private String reconciliationReference;
    
    @Column(name = "INVOICE_DATE")
    private LocalDate invoiceDate;
    
    @Column(name = "COMPLETED_AT")
    private LocalDateTime completedAt;
    
    @Size(max = 4000, message = "Description must be less than 4000 characters")
    @Column(name = "DESCRIPTION", length = 4000)
    private String description;
    
    @Column(name = "DUE_DATE")
    @Temporal(TemporalType.DATE)
    private LocalDate dueDate;
    
    @Lob
    @Column(name = "ERROR_DETAILS")
    private String errorDetails;
    
    @Column(name = "IS_APPROVED")
    private boolean approved;
    
    @Size(max = 50, message = "Approved by must be less than 50 characters")
    @Column(name = "APPROVED_BY", length = 50)
    private String approvedBy;
    
    @Column(name = "APPROVED_AT")
    private LocalDateTime approvedAt;
    
    @Lob
    @Column(name = "APPROVAL_NOTES")
    private String approvalNotes;
    
    @Column(name = "IS_REJECTED")
    private boolean rejected;
    
    @Size(max = 50, message = "Rejected by must be less than 50 characters")
    @Column(name = "REJECTED_BY", length = 50)
    private String rejectedBy;
    
    @Column(name = "REJECTED_AT")
    private LocalDateTime rejectedAt;
    
    @Lob
    @Column(name = "REJECTION_REASON")
    private String rejectionReason;
    
    @Column(name = "IS_EXPORTED")
    private boolean exported;
    
    @Column(name = "EXPORTED_AT")
    private LocalDateTime exportedAt;
    
    @Column(name = "IS_REVERSAL")
    private boolean reversal;
    
    @Lob
    @Column(name = "REVERSAL_REASON")
    private String reversalReason;
    
    @Size(max = 255, message = "Custom field 2 must be less than 255 characters")
    @Column(name = "CUSTOM_FIELD2", length = 255)
    private String customField2;
    
    @Size(max = 255, message = "Custom field 3 must be less than 255 characters")
    @Column(name = "CUSTOM_FIELD3", length = 255)
    private String customField3;
    
    @Size(max = 255, message = "Custom field 4 must be less than 255 characters")
    @Column(name = "CUSTOM_FIELD4", length = 255)
    private String customField4;
    
    @Size(max = 255, message = "Custom field 5 must be less than 255 characters")
    @Column(name = "CUSTOM_FIELD5", length = 255)
    private String customField5;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // All fields are now declared with JPA annotations above
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { 
        this.vendor = vendor;
        if (vendor != null) {
            // Set the vendorId from the vendor's vendorNumber
            this.vendorId = vendor.getVendorNumber();
        } else {
            this.vendorId = null;
        }
    }
    
    /**
     * Get the vendor ID.
     * @return the vendor ID as a String
     */
    public String getVendorId() {
        return vendorId;
    }
    
    /**
     * Set the vendor ID directly. This is typically not called directly
     * as the vendorId is managed through the vendor relationship.
     * @param vendorId the vendor ID as a String
     */
    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }
    
    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    
    public String getVendorEmail() { return vendorEmail; }
    public void setVendorEmail(String vendorEmail) { this.vendorEmail = vendorEmail; }
    
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    
    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
    
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    
    public PaymentStatus getStatus() { return status; }
    
    /**
     * Set the status using a PaymentStatus enum value.
     * @param status the status to set
     */
    public void setStatus(PaymentStatus status) { 
        this.status = status != null ? status : PaymentStatus.PENDING;
    }
    
    /**
     * Set the status using a string value.
     * This is a convenience method for cases where the status comes as a string.
     * @param statusStr the status string to set
     */
    public void setStatus(String statusStr) {
        this.status = fromStatusString(statusStr);
    }
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public String getBankReference() { return bankReference; }
    public void setBankReference(String bankReference) { this.bankReference = bankReference; }
    
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public boolean isNotificationSent() { return notificationSent; }
    public void setNotificationSent(boolean notificationSent) { this.notificationSent = notificationSent; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public String getErrorDetails() { return errorDetails; }
    public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }
    
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public boolean isReconciled() { return reconciled; }
    public void setReconciled(boolean reconciled) { this.reconciled = reconciled; }
    
    public LocalDateTime getReconciliationDate() { return reconciliationDate; }
    public void setReconciliationDate(LocalDateTime reconciliationDate) { this.reconciliationDate = reconciliationDate; }
    
    // Alias for getStatus() for backward compatibility
    public String getPaymentStatus() { return status != null ? status.name() : null; }
    /**
     * Alias for setStatus() for backward compatibility
     * @param status the status string to set (case-insensitive)
     * @throws IllegalArgumentException if the status string is not a valid PaymentStatus
     */
    public void setPaymentStatus(String status) { 
        if (status == null || status.trim().isEmpty()) {
            this.status = PaymentStatus.PENDING;
            return;
        }
        try {
            this.status = PaymentStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid payment status: {}", status);
            throw new IllegalArgumentException("Invalid payment status: " + status + 
                ". Valid values are: " + Arrays.toString(PaymentStatus.values()));
        }
    }
    
    @Override
    public String toString() {
        return "VendorPayment{" +
                "vendorId='" + vendorId + '\'' +
                ", vendorName='" + vendorName + '\'' +
                ", vendorEmail='" + vendorEmail + '\'' +
                ", bankAccount='" + maskBankAccount(bankAccount) + '\'' +
                ", invoiceNumber='" + invoiceNumber + '\'' +
                ", amount=" + amount +
                ", paymentDate=" + paymentDate +
                ", paymentReference='" + paymentReference + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", status='" + status + '\'' +
                ", notificationSent=" + notificationSent +
                '}';
    }
    
    /**
     * Masks the bank account number for security in logs
     * @param accountNumber The full bank account number
     * @return Masked account number showing only last 4 digits
     */
    private String maskBankAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
