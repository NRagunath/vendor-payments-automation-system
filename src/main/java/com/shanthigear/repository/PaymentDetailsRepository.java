package com.shanthigear.repository;

import com.shanthigear.model.PaymentDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentDetailsRepository extends JpaRepository<PaymentDetails, Long> {
    List<PaymentDetails> findByVendorNumber(String vendorNumber);
    
    Optional<PaymentDetails> findByPaymentReference(String paymentReference);
    
    List<PaymentDetails> findByStatus(String status);
    
    boolean existsByPaymentReference(String paymentReference);
}
