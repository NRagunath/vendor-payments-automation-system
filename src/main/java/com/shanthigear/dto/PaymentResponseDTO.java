package com.shanthigear.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.shanthigear.model.PaymentStatus;
import com.shanthigear.model.VendorPayment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for payment responses.
 * Provides a clean API response format and includes only necessary fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponseDTO {

    private String paymentReference;
    private String transactionId;
    private String vendorId;
    private String vendorName;
    private String vendorEmail;
    private String bankAccount;
    private String invoiceNumber;
    private BigDecimal amount;
    private String status; // Stores the string representation of PaymentStatus
    private boolean notificationSent;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate paymentDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private String notes;

    /**
     * Creates a PaymentResponseDTO from a VendorPayment entity.
     *
     * @param payment the VendorPayment entity to convert
     * @return a new PaymentResponseDTO
     */
    public static PaymentResponseDTO fromEntity(VendorPayment payment) {
        if (payment == null) {
            return null;
        }
        return builder()
                .paymentReference(payment.getPaymentReference())
                .transactionId(payment.getTransactionId())
                .vendorId(payment.getVendorId())
                .vendorName(payment.getVendorName())
                .vendorEmail(payment.getVendorEmail())
                .bankAccount(payment.getBankAccount())
                .invoiceNumber(payment.getInvoiceNumber())
                .amount(payment.getAmount())
                .status(payment.getStatus() != null ? payment.getStatus().toString() : PaymentStatus.PENDING.name())
                .notificationSent(payment.isNotificationSent())
                .paymentDate(payment.getPaymentDate())
                .dueDate(payment.getDueDate())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .notes(payment.getNotes())
                .build();
    }
}
