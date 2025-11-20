package com.shanthigear.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import com.shanthigear.model.Vendor;
import com.shanthigear.repository.VendorRepository;

@SqlGroup({
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:db/test-data.sql"),
    @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:db/cleanup.sql")
})
class PaymentIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private VendorRepository vendorRepository;

    private String vendorId = "VENDOR_PAY_001";

    @BeforeEach
    void setUp() {
        // Setup test vendor using builder pattern
        Vendor vendor = Vendor.builder()
            .vendorNumber(vendorId)
            .vendorName("Payment Test Vendor")
            .emailAddress("payment@test.com")
            .bankAccountNum("9876543210")
            .bankName("Payment Test Bank")
            .ifscCode("PAYT0123456")
            .build();
        vendorRepository.save(vendor);
    }

    @Test
    void processPayment_WithValidRequest_ReturnsSuccess() throws Exception {
        String paymentRequest = String.format("""
        {
            "vendorId": "%s",
            "amount": 1500.75,
            "currency": "INR",
            "invoiceNumber": "INV-2025-001",
            "description": "Test payment integration"
        }
        """, vendorId);

        mockMvc.perform(post("/api/v1/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.paymentId").isNotEmpty());
    }

    @Test
    void getPaymentStatus_WithExistingPayment_ReturnsStatus() throws Exception {
        // First create a payment
        String paymentRequest = String.format("""
        {
            "vendorId": "%s",
            "amount": 2000.00,
            "invoiceNumber": "INV-2025-002"
        }
        """, vendorId);

        String response = mockMvc.perform(post("/api/v1/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest))
                .andReturn().getResponse().getContentAsString();
        String paymentId = response.split("paymentId\":\"")[1].split("\"")[0];

        // Then get its status
        mockMvc.perform(get("/api/v1/payments/status/{paymentId}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId))
                .andExpect(jsonPath("$.status").isNotEmpty());
    }

    @Test
    void getPaymentStatus_WithNonExistentPayment_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/payments/status/non-existent-payment"))
                .andExpect(status().isNotFound());
    }

    @Test
    void processBatchPayments_WithValidRequest_ReturnsSuccess() throws Exception {
        String batchRequest = String.format("""
        [
            {
                "vendorId": "%s",
                "amount": 1000.00,
                "invoiceNumber": "INV-2025-003"
            },
            {
                "vendorId": "%s",
                "amount": 1500.00,
                "invoiceNumber": "INV-2025-004"
            }
        ]
        """, vendorId, vendorId);

        mockMvc.perform(post("/api/v1/payments/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").isNotEmpty())
                .andExpect(jsonPath("$[1].status").isNotEmpty());
    }
}
