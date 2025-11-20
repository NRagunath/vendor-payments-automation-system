package com.shanthigear.model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payment_details")
public class PaymentDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "vendor_number", nullable = false)
    private String vendorNumber;
    
    @Column(name = "vendor_name", nullable = false)
    private String vendorName;
    
    @Column(name = "vendor_site")
    private String vendorSite;
    
    @Column(name = "pay_group", nullable = false)
    private String payGroup;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "payment_reference", unique = true)
    private String paymentReference;
    
    @Column(length = 20)
    private String status = "PENDING";
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
    // Additional fields can be added as needed
    
    @PrePersist
    protected void onCreate() {
        if (this.paymentReference == null) {
            // Generate a payment reference if not provided
            this.paymentReference = "PAY" + System.currentTimeMillis();
        }
    }
}
