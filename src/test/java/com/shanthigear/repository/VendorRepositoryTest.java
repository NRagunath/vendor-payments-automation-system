package com.shanthigear.repository;

import com.shanthigear.model.Vendor;
import com.shanthigear.service.VendorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class VendorRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private VendorRepository vendorRepository;
    
    @Autowired
    private VendorService vendorService;

    @Test
    void findByVendorId_WhenVendorExists_ReturnsVendor() {
        // Given
        Vendor vendor = Vendor.builder()
            .vendorNumber("VENDOR123")
            .vendorName("Test Vendor")
            .bankAccountNum("1234567890")
            .bankName("Test Bank")
            .ifscCode("TEST0123456")
            .build();
        entityManager.persistAndFlush(vendor);

        // When
        Optional<Vendor> found = vendorRepository.findByVendorNumber("VENDOR123");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getVendorNumber()).isEqualTo("VENDOR123");
    }

    @Test
    void findByVendorId_WhenVendorNotExists_ReturnsEmpty() {
        // When
        Optional<Vendor> found = vendorRepository.findByVendorNumber("NONEXISTENT");


        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void findByBankAccount_WhenVendorExists_ReturnsVendor() {
        // Given
        Vendor vendor = Vendor.builder()
            .vendorNumber("VENDOR123")
            .vendorName("Test Vendor")
            .bankAccountNum("1234567890")
            .bankName("Test Bank")
            .ifscCode("TEST0123456")
            .build();
        entityManager.persistAndFlush(vendor);

        // When
        List<Vendor> found = vendorRepository.findByBankAccountNum("1234567890");

        // Then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getBankAccountNum()).isEqualTo("1234567890");
    }

    @Test
    void findByEmail_WhenVendorExists_ReturnsVendor() {
        // Given
        Vendor vendor = Vendor.builder()
            .vendorNumber("VENDOR123")
            .vendorName("Test Vendor")
            .emailAddress("vendor@example.com")
            .build();
        entityManager.persistAndFlush(vendor);

        // When
        List<Vendor> found = vendorRepository.findByEmailAddress("vendor@example.com");

        // Then
        assertThat(found).isNotEmpty();
        assertThat(found.get(0).getEmailAddress()).isEqualTo("vendor@example.com");
    }

    @Test
    void isEligibleForH2H_WhenVendorHasRequiredFields_ReturnsTrue() {
        // Given - Vendor with all required fields for H2H
        Vendor eligibleVendor = Vendor.builder()
            .vendorNumber("VENDOR123")
            .vendorName("Eligible Vendor")
            .bankAccountNum("1234567890")
            .bankName("Test Bank")
            .ifscCode("TEST0123456")
            .payGroup("H2H_PAY_GROUP")
            .emailAddress("vendor@example.com")
            .build();
        entityManager.persist(eligibleVendor);
        entityManager.flush();
        
        // When - Check if vendor is eligible for H2H
        boolean isEligible = vendorService.isEligibleForH2H("VENDOR123");
        
        // Then - Verify the vendor is eligible
        assertThat(isEligible).isTrue();
    }
    
    @Test
    void isEligibleForH2H_WhenVendorMissingRequiredFields_ReturnsFalse() {
        // Given - Vendor missing required fields for H2H (no email or pay group)
        Vendor ineligibleVendor = Vendor.builder()
            .vendorNumber("VENDOR456")
            .vendorName("Ineligible Vendor")
            .bankAccountNum("0987654321")
            .bankName("Test Bank")
            .ifscCode("TEST0654321")
            .build();
        entityManager.persist(ineligibleVendor);
        entityManager.flush();
        
        // When - Check if vendor is eligible for H2H
        boolean isEligible = vendorService.isEligibleForH2H("VENDOR456");
        
        // Then - Verify the vendor is not eligible
        assertThat(isEligible).isFalse();
    }

    @Test
    void deleteByVendorId_WhenVendorExists_DeletesVendor() {
        // Given
        Vendor vendor = Vendor.builder()
            .vendorNumber("VENDOR123")
            .vendorName("Test Vendor")
            .build();
        entityManager.persistAndFlush(vendor);

        // When
        vendorRepository.deleteByVendorNumber("VENDOR123");
        entityManager.flush();

        // Then
        assertThat(vendorRepository.findByVendorNumber("VENDOR123")).isEmpty();
    }
}
