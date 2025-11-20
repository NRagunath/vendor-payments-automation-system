package com.shanthigear.service.impl;

import com.shanthigear.model.Vendor;
import com.shanthigear.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorServiceImplTest {

    @Mock
    private VendorRepository vendorRepository;

    @InjectMocks
    private VendorServiceImpl vendorService;

    private Vendor testVendor;

    @BeforeEach
    void setUp() {
        testVendor = new Vendor();
        testVendor.setVendorNumber("12345");
        testVendor.setPayGroup("MSME");
    }

    @Test
    void isEligibleForH2H_WithEligibleVendor_ReturnsTrue() {
        // Arrange - Set up a vendor with all required fields for H2H
        testVendor.setBankAccountNum("1234567890");
        testVendor.setBankName("Test Bank");
        testVendor.setIfscCode("TEST0123456");
        testVendor.setPayGroup("H2H_PAY_GROUP");
        testVendor.setEmailAddress("vendor@example.com");
        
        when(vendorRepository.findByVendorNumber(anyString())).thenReturn(Optional.of(testVendor));
        
        // Act & Assert
        assertTrue(vendorService.isEligibleForH2H("12345"));
    }

    @Test
    void isEligibleForH2H_WithMissingRequiredFields_ReturnsFalse() {
        // Arrange - Set up a vendor missing required fields for H2H
        testVendor.setBankAccountNum(null); // Missing bank account number
        testVendor.setBankName("Test Bank");
        testVendor.setIfscCode("TEST0123456");
        testVendor.setPayGroup("H2H_PAY_GROUP");
        testVendor.setEmailAddress("vendor@example.com");
        
        when(vendorRepository.findByVendorNumber(anyString())).thenReturn(Optional.of(testVendor));
        
        // Act & Assert
        assertFalse(vendorService.isEligibleForH2H("12345"));
    }

    @Test
    void isEligibleForH2H_WithNonExistentVendor_ReturnsFalse() {
        // Arrange
        when(vendorRepository.findByVendorNumber(anyString())).thenReturn(Optional.empty());
        
        // Act & Assert
        assertFalse(vendorService.isEligibleForH2H("nonexistent"));
    }
}
