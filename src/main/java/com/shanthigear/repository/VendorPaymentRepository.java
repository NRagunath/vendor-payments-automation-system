package com.shanthigear.repository;

import com.shanthigear.model.PaymentStatus;
import com.shanthigear.model.Vendor;
import com.shanthigear.model.VendorPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing vendor payment records.
 */
public interface VendorPaymentRepository extends JpaRepository<VendorPayment, Long> {
    
    // Find by reference methods
    Optional<VendorPayment> findByReferenceNumber(String referenceNumber);
    boolean existsByReferenceNumber(String referenceNumber);
    Optional<VendorPayment> findByPaymentReference(String paymentReference);
    boolean existsByPaymentReference(String paymentReference);
    Optional<VendorPayment> findByBankReference(String bankReference);
    Optional<VendorPayment> findByTransactionId(String transactionId);
    
    // Find by vendor methods
    List<VendorPayment> findByVendor(Vendor vendor);
    Page<VendorPayment> findByVendor(Vendor vendor, Pageable pageable);
    List<VendorPayment> findByVendorId(String vendorId);
    Page<VendorPayment> findByVendorId(String vendorId, Pageable pageable);
    List<VendorPayment> findByVendorIdOrderByCreatedAtDesc(String vendorId);
    List<VendorPayment> findTop10ByVendorOrderByPaymentDateDesc(Vendor vendor);
    
    // Find by status methods
    List<VendorPayment> findByStatus(PaymentStatus status);
    Page<VendorPayment> findByStatus(PaymentStatus status, Pageable pageable);
    @Query("SELECT vp FROM VendorPayment vp WHERE vp.status = :status")
    List<VendorPayment> findByStatus(@Param("status") String status);
    @Query("SELECT vp FROM VendorPayment vp WHERE vp.status = :status")
    Page<VendorPayment> findByStatus(@Param("status") String status, Pageable pageable);
    
    // H2H related methods
    List<VendorPayment> findByH2hReference(String h2hReference);
    
    /**
     * Find all payments associated with a specific batch ID.
     *
     * @param batchId the batch ID to search for
     * @return list of payments in the specified batch
     */
    List<VendorPayment> findByBatchId(String batchId);
    
    @Query("SELECT p FROM VendorPayment p WHERE p.h2hProcessed = :processed AND p.h2hProcessedAt BETWEEN :startDate AND :endDate")
    List<VendorPayment> findByH2hProcessedAndH2hProcessedAtBetween(
        @Param("processed") boolean processed,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // Combined finder methods
    @Query("SELECT vp FROM VendorPayment vp WHERE vp.vendor = :vendor AND vp.status = :status")
    List<VendorPayment> findByVendorAndStatus(
        @Param("vendor") Vendor vendor, 
        @Param("status") String status
    );
    
    @Query("SELECT vp FROM VendorPayment vp WHERE vp.vendor.vendorId = :vendorId AND vp.status = :status")
    List<VendorPayment> findByVendorIdAndStatus(
        @Param("vendorId") String vendorId, 
        @Param("status") String status
    );
    
    List<VendorPayment> findByVendorAndStatus(Vendor vendor, PaymentStatus status);
    List<VendorPayment> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime date);
    
    // Date range queries
    @Query("SELECT vp FROM VendorPayment vp WHERE vp.paymentDate BETWEEN :startDate AND :endDate")
    List<VendorPayment> findByPaymentDateBetween(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );
}
