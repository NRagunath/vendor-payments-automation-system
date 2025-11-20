package com.shanthigear.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a batch of payments that are processed together.
 * This is typically used for bulk payment processing operations.
 */
/**
 * Represents a batch of payments that are processed together.
 * This is typically used for bulk payment processing operations.
 */
@Entity
@Table(name = "BATCH_PAYMENT")
@SequenceGenerator(name = "batch_payment_seq", sequenceName = "BATCH_PAYMENT_SEQ", allocationSize = 1)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchPayment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "batch_payment_seq")
    @Column(name = "ID")
    private Long id;
    
    @Size(max = 50, message = "Batch reference must be less than 50 characters")
    @Column(name = "BATCH_REFERENCE", nullable = false, unique = true, length = 50)
    private String batchReference;
    
    @Size(max = 4000, message = "Description must be less than 4000 characters")
    @Column(name = "DESCRIPTION", length = 4000)
    private String description;
    
    @Column(name = "TOTAL_AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "PAYMENT_COUNT", nullable = false)
    private int paymentCount;
    
    @Column(name = "SUCCESS_COUNT")
    private Integer successCount;
    
    @Column(name = "FAILURE_COUNT")
    private Integer failureCount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private BatchStatus status;
    
    @Size(max = 50, message = "Created by must be less than 50 characters")
    @Column(name = "CREATED_BY", length = 50)
    private String createdBy;
    
    @Column(name = "initiated_at")
    private LocalDateTime initiatedAt;
    
    @Column(name = "COMPLETED_AT")
    private LocalDateTime completedAt;
    
    @Lob
    @Column(name = "ERROR_MESSAGE")
    private String errorMessage;
    
    @OneToMany(mappedBy = "batchPayment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<VendorPayment> payments = new ArrayList<>();
    
    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = BatchStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
