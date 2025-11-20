package com.shanthigear.util;

import com.shanthigear.model.Vendor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VendorUtilsTest {

    @Test
    void maskSensitiveData_ShouldMaskBankAccount() {
        Vendor vendor = Vendor.builder()
            .vendorNumber("VENDOR123")
            .vendorName("Test Vendor")
            .bankAccountNum("1234567890")
            .bankName("Test Bank")
            .ifscCode("TEST0123456")
            .build();
        
        String masked = VendorUtils.maskSensitiveData(vendor);
        
        assertTrue(masked.contains("bankAccountNum='****7890'"));
    }

    @Test
    void maskSensitiveData_ShouldHandleNullBankAccount() {
        Vendor vendor = Vendor.builder()
            .vendorNumber("VENDOR123")
            .vendorName("Test Vendor")
            .bankAccountNum(null)
            .bankName("Test Bank")
            .build();
        
        String masked = VendorUtils.maskSensitiveData(vendor);
        
        assertTrue(masked.contains("bankAccountNum='null'"));
    }

    @Test
    void normalizeVendorData_ShouldTrimAndUppercase() {
        Vendor vendor = Vendor.builder()
            .vendorNumber("  VENDOR123  ")
            .vendorName("  test vendor  ")
            .emailAddress(" TEST@example.com  ")
            .bankAccountNum(" 1234567890 ")
            .bankName("  Test Bank  ")
            .branch("  Main Branch  ")
            .ifscCode(" test0123456 ")
            .build();
        
        Vendor normalizedVendor = VendorUtils.normalizeVendor(vendor);
        
        assertEquals("VENDOR123", normalizedVendor.getVendorNumber());
        assertEquals("test vendor", normalizedVendor.getVendorName());
        assertEquals("test@example.com", normalizedVendor.getEmailAddress());
        assertEquals("1234567890", normalizedVendor.getBankAccountNum());
        assertEquals("Test Bank", normalizedVendor.getBankName());
        assertEquals("Main Branch", normalizedVendor.getBranch());
        assertEquals("TEST0123456", normalizedVendor.getIfscCode());
    }
    
    @Test
    void maskSensitiveData_ShouldMaskEmail() {
        Vendor vendor = Vendor.builder()
            .vendorNumber("VENDOR123")
            .emailAddress("test.user@example.com")
            .build();
        
        String masked = VendorUtils.maskSensitiveData(vendor);
        
        assertTrue(masked.contains("emailAddress='t*****r@example.com'"));
    }
    
    
    @Test
    void maskSensitiveData_ShouldMaskVendorId() {
        Vendor vendor = Vendor.builder()
            .vendorNumber("VENDOR123")
            .build();
        
        String masked = VendorUtils.maskSensitiveData(vendor);
        
        assertTrue(masked.contains("vendorNumber='****R123'"));
    }
}
