package com.shanthigear.integration;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.shanthigear.model.Payment;
import com.shanthigear.model.PaymentStatus;
import com.shanthigear.model.Vendor;
import com.shanthigear.repository.PaymentRepository;
import com.shanthigear.repository.VendorRepository;
import com.shanthigear.service.PaymentGatewayService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentProcessingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private PaymentGatewayService paymentGatewayService;

    private String testVendorId = "VENDOR-PAY-001";
    private String testPaymentId = "PAY-" + UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        // Clear existing test data
        paymentRepository.deleteAll();
        vendorRepository.deleteAll();

        // Create a test vendor using builder pattern
        Vendor vendor = Vendor.builder()
            .vendorNumber(testVendorId)
            .vendorName("Payment Test Vendor")
            .emailAddress("payment@test.com")
            .bankAccountNum("9876543210")
            .bankName("Test Bank")
            .ifscCode("TEST0123456")
            .build();
        vendorRepository.save(vendor);

        // Mock payment gateway response
        when(paymentGatewayService.processPayment(any(Payment.class)))
            .thenAnswer(invocation -> {
                Payment payment = invocation.getArgument(0);
                payment.setStatus(PaymentStatus.PROCESSING);
                payment.setPaymentId("PAY-" + System.currentTimeMillis());
                return payment;
            });

        when(paymentGatewayService.checkPaymentStatus(anyString()))
            .thenReturn(PaymentStatus.COMPLETED);
    }

    @Test
    void processPayment_WithValidRequest_ShouldProcessSuccessfully() throws Exception {
        String paymentRequest = String.format("""
        {
            "vendorId": "%s",
            "amount": 1500.75,
            "currency": "INR",
            "invoiceNumber": "INV-2025-001",
            "description": "Test payment integration"
        }
        """, testVendorId);

        mockMvc.perform(post("/api/v1/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.paymentId").isNotEmpty())
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.vendorId").value(testVendorId));
    }

    @Test
    void processPayment_WithNonExistentVendor_ShouldReturnNotFound() throws Exception {
        String paymentRequest = """
        {
            "vendorId": "NON-EXISTENT-VENDOR",
            "amount": 1000,
            "currency": "INR",
            "invoiceNumber": "INV-2025-002"
        }
        """;

        mockMvc.perform(post("/api/v1/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Vendor not found with id: NON-EXISTENT-VENDOR"));
    }

    @Test
    void getPaymentStatus_WithExistingPayment_ShouldReturnStatus() throws Exception {
        // Create a test payment
        Payment payment = new Payment();
        payment.setPaymentId(testPaymentId);
        payment.setVendorId(testVendorId);
        payment.setAmount(BigDecimal.valueOf(2000.00));
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // When/Then
        mockMvc.perform(get("/api/v1/payments/status/{paymentId}", testPaymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(testPaymentId))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Verify payment gateway was called to check status
        verify(paymentGatewayService, times(1)).checkPaymentStatus(testPaymentId);
    }

    @Test
    void processBatchPayments_WithValidRequest_ShouldProcessAllPayments() throws Exception {
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
        """, testVendorId, testVendorId);

        mockMvc.perform(post("/api/v1/payments/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].status").value("PROCESSING"))
                .andExpect(jsonPath("$[1].status").value("PROCESSING"));

        // Verify payment gateway was called for each payment
        verify(paymentGatewayService, times(2)).processPayment(any(Payment.class));
    }

    @Test
    void exportPayments_WithDateRange_ShouldReturnCsvFile() throws Exception {
        // Create test payments
        createTestPayments();

        // When/Then
        mockMvc.perform(get("/api/v1/payments/export")
                .param("startDate", "2025-01-01")
                .param("endDate", "2025-12-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("payments-export-")))
                .andExpect(header().string("Content-Disposition", containsString(".csv")))
                .andExpect(content().contentType("text/csv"));
    }

    private void createTestPayments() {
        for (int i = 0; i < 5; i++) {
            Payment payment = new Payment();
            payment.setPaymentId("PAY-TEST-" + i);
            payment.setVendorId(testVendorId);
            payment.setAmount(BigDecimal.valueOf(1000 + (i * 100)));
            payment.setCurrency("INR");
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setInvoiceNumber("INV-TEST-" + i);
            payment.setCreatedAt(LocalDateTime.now().minusDays(i));
            paymentRepository.save(payment);
        }
    }
}
