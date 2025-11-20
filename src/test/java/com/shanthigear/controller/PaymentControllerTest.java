package com.shanthigear.controller;

import com.shanthigear.model.PaymentStatus;
import com.shanthigear.model.Vendor;
import com.shanthigear.model.VendorPayment;
import com.shanthigear.repository.VendorRepository;
import com.shanthigear.service.OracleHostToHostService;
import com.shanthigear.service.PaymentProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentProcessingService paymentProcessingService;
    
    @Mock
    private VendorRepository vendorRepository;
    
    @Mock
    private OracleHostToHostService oracleHostToHostService;
    
    @InjectMocks
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
    }

    @Test
    void createAndProcessPayment_WithValidRequest_ReturnsCreated() throws Exception {
        // Given
        String vendorNumber = "VEND123";
        String requestJson = """
        {
            "vendorNumber": "VEND123",
            "amount": 1000.50,
            "invoiceNumber": "INV-2025-001",
            "paymentDate": "2025-04-01"
        }
        """;

        Vendor vendor = Vendor.builder()
            .vendorNumber(vendorNumber)
            .vendorName("Test Vendor")
            .emailAddress("test@example.com")
            .build();
            
        VendorPayment mockPayment = new VendorPayment();
        mockPayment.setVendor(vendor);
        mockPayment.setAmount(new BigDecimal("1000.50"));
        mockPayment.setInvoiceNumber("INV-2025-001");
        mockPayment.setPaymentDate(LocalDate.of(2025, 4, 1));
        mockPayment.setStatus(PaymentStatus.PENDING);
        mockPayment.setPaymentReference("PAY-123");

        when(paymentProcessingService.processPayment(any(VendorPayment.class))).thenReturn(mockPayment);
        when(vendorRepository.findByVendorNumber(anyString())).thenReturn(Optional.of(vendor));

        // When/Then
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentReference").value("PAY-123"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createAndProcessPayment_WithInvalidAmount_ReturnsBadRequest() throws Exception {
        // Given
        String invalidRequestJson = """
        {
            "vendorNumber": "VEND123",
            "amount": 0,
            "invoiceNumber": "INV-2025-001"
        }
        """;

        // When/Then
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Payment amount must be greater than zero"));
    }

    @Test
    void getPayment_WithValidReference_ReturnsPayment() throws Exception {
        // Given
        String reference = "PAY-123";
        VendorPayment payment = new VendorPayment();
        payment.setPaymentReference(reference);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setAmount(new BigDecimal("1000.50"));
        payment.setInvoiceNumber("INV-2025-001");

        when(oracleHostToHostService.getPaymentByReference(reference)).thenReturn(Optional.of(payment));

        // When/Then
        mockMvc.perform(get("/api/payments/{reference}", reference))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentReference").value(reference))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getVendorPayments_WithValidVendorId_ReturnsPayments() throws Exception {
        // Given
        String vendorNumber = "VEND123";
        Vendor vendor = Vendor.builder()
            .vendorNumber(vendorNumber)
            .vendorName("Test Vendor")
            .emailAddress("test@example.com")
            .build();
        
        List<VendorPayment> payments = new ArrayList<>();
        VendorPayment payment = new VendorPayment();
        payment.setVendor(vendor);
        payment.setStatus(PaymentStatus.COMPLETED);
        payments.add(payment);
        
        when(vendorRepository.findByVendorNumber(vendorNumber)).thenReturn(Optional.of(vendor));
        when(oracleHostToHostService.getPaymentsByVendor(vendorNumber)).thenReturn(payments);
        
        // When/Then
        mockMvc.perform(get("/api/payments/vendors/{vendorNumber}", vendorNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }
    
    @Test
    void updatePaymentStatus_WithValidStatus_ReturnsUpdatedPayment() throws Exception {
        // Given
        String reference = "PAY-123";
        String status = "COMPLETED";
        
        VendorPayment payment = new VendorPayment();
        payment.setPaymentReference(reference);
        payment.setStatus(PaymentStatus.COMPLETED);
        
        when(oracleHostToHostService.updatePaymentStatus(reference, status, null)).thenReturn(payment);
        
        // When/Then
        mockMvc.perform(put("/api/payments/{reference}/status", reference)
                .param("status", status))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
