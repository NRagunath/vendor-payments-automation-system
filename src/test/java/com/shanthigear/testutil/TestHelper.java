package com.shanthigear.testutil;

import com.shanthigear.model.Vendor;

import java.time.LocalDate;
import java.util.UUID;

public class TestHelper {

    public static Vendor createTestVendor() {
        return createTestVendor("TEST" + UUID.randomUUID().toString().substring(0, 8));
    }

    public static Vendor createTestVendor(String vendorNumber) {
        return Vendor.builder()
            .vendorNumber(vendorNumber)
            .vendorName("Test Vendor " + vendorNumber)
            .vendorSite("SITE" + vendorNumber)
            .payGroup("STANDARD")
            .emailAddress("vendor" + vendorNumber + "@test.com")
            .accountNumber("12345678" + (1000 + (int)(Math.random() * 9000)))
            .bankName("Test Bank")
            .ifscCode("TEST0" + (100000 + (int)(Math.random() * 900000)))
            .creationDate(LocalDate.now())
            .build();
    }

    public static String asJsonString(final Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
