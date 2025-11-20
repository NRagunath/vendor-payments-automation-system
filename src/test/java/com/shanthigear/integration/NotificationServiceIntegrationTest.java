package com.shanthigear.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.shanthigear.model.Vendor;
import com.shanthigear.model.VendorPayment;
import com.shanthigear.model.PaymentSummary;
import com.shanthigear.model.PaymentStatus;
import com.shanthigear.service.EmailService;
import com.shanthigear.service.NotificationService;
import com.shanthigear.model.EmailSendingResult;
import com.shanthigear.model.PaymentException;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @MockBean
    private EmailService emailService;

    private Vendor testVendor;
    private VendorPayment testPayment;

    @BeforeEach
    void setUp() {
        // Setup test vendor using builder pattern
        testVendor = Vendor.builder()
            .vendorNumber("VENDOR-NOTIF-001")
            .vendorName("Notification Test Vendor")
            .emailAddress("notification@test.com")
            .build();

        // Setup test payment using builder pattern
        testPayment = VendorPayment.builder()
            .id(1L)
            .vendor(testVendor)
            .vendorId(testVendor.getVendorNumber())
            .vendorName(testVendor.getVendorName())
            .vendorEmail(testVendor.getEmailAddress())
            .amount(new BigDecimal("1500.75"))
            .paymentDate(java.time.LocalDate.now())
            .referenceNumber("PAY-12345")
            .paymentReference("INV-2025-001")
            .status(PaymentStatus.PROCESSING)
            .build();
    }

    @Test
    void sendPaymentNotification_ShouldSendEmail() {
        // Given
        CompletableFuture<EmailSendingResult> future = CompletableFuture.completedFuture(
            new EmailSendingResult(true, "Email sent successfully")
        );
        when(emailService.sendEmail(any(VendorPayment.class))).thenReturn(future);

        // When
        notificationService.sendPaymentNotification(testVendor, testPayment);

        // Then verify email was sent
        verify(emailService, times(1)).sendEmail(any(VendorPayment.class));
        
        // Verify payment was updated with vendor details
        assertEquals(testVendor.getVendorNumber(), testPayment.getVendorId());
        assertEquals(testVendor.getVendorName(), testPayment.getVendorName());
        assertEquals(testVendor.getEmailAddress(), testPayment.getVendorEmail());
    }
    
    @Test
    void sendPaymentConfirmation_ShouldSendEmail() {
        // Given
        CompletableFuture<EmailSendingResult> future = CompletableFuture.completedFuture(
            new EmailSendingResult(true, "Email sent successfully")
        );
        when(emailService.sendEmail(any(VendorPayment.class))).thenReturn(future);

        // When
        notificationService.sendPaymentConfirmation(testPayment);

        // Then verify email was sent
        verify(emailService, times(1)).sendEmail(any(VendorPayment.class));
        
        // Verify payment status is set to COMPLETED
        assertEquals(PaymentStatus.COMPLETED, testPayment.getStatus());
    }

    
    @Test
    void sendPaymentFailure_ShouldSendEmail() {
        // Given
        String reason = "Insufficient funds";
        CompletableFuture<EmailSendingResult> future = CompletableFuture.completedFuture(
            new EmailSendingResult(true, "Email sent successfully")
        );
        when(emailService.sendEmail(any(VendorPayment.class))).thenReturn(future);
        
        // When
        notificationService.sendPaymentFailure(testPayment, reason);

        // Then
        verify(emailService, times(1)).sendEmail(any(VendorPayment.class));
        assertEquals(reason, testPayment.getRemarks());
        assertEquals(PaymentStatus.FAILED, testPayment.getStatus());
    }
    
    @Test
    void sendOverduePaymentNotification_ShouldSendEmail() {
        // Given
        CompletableFuture<EmailSendingResult> future = CompletableFuture.completedFuture(
            new EmailSendingResult(true, "Email sent successfully")
        );
        when(emailService.sendEmail(any(VendorPayment.class))).thenReturn(future);

        // When
        notificationService.sendOverduePaymentNotification(testPayment);

        // Then
        verify(emailService, times(1)).sendEmail(any(VendorPayment.class));
        assertNotNull(testPayment.getRemarks());
        assertTrue(testPayment.getRemarks().contains("overdue"));
    }
    
    @Test
    void sendPaymentSummary_ShouldSendEmail() {
        // Given
        PaymentSummary summary = new PaymentSummary();
        summary.setTotalPayments(10);
        summary.setTotalAmount(new BigDecimal("15000.0"));
        summary.setCurrency("USD");
        summary.setPaymentDate(java.time.LocalDate.now());
        summary.setBatchId("BATCH-123");
        
        // Create a sample payment detail
        PaymentSummary.PaymentDetail detail = new PaymentSummary.PaymentDetail();
        detail.setPaymentReference("PAY-REF-001");
        detail.setVendorId("VENDOR-001");
        detail.setVendorName("Test Vendor");
        detail.setAmount(new BigDecimal("1500.00"));
        detail.setStatus("COMPLETED");
        detail.setBankAccount("1234567890");
        detail.setIfscCode("HDFC0001234");
        
        summary.setPaymentDetails(List.of(detail));
        
        List<String> recipients = Arrays.asList("finance@example.com", "accounting@example.com");

        // When
        notificationService.sendPaymentSummary(summary, recipients);

        // Then - Verify the email service was called
        verify(emailService, times(1)).sendEmail(any(VendorPayment.class));
    }
    
    @Test
    void sendExceptionReport_ShouldSendEmail() {
        // Given
        PaymentException exception1 = new PaymentException();
        exception1.setErrorMessage("Insufficient funds");
        exception1.setSeverity("HIGH");
        exception1.setStatus("OPEN");
        exception1.setModule("PAYMENT_PROCESSING");
        exception1.setTimestamp(LocalDateTime.now());
        
        PaymentException exception2 = new PaymentException();
        exception2.setErrorMessage("Invalid account number");
        exception2.setSeverity("CRITICAL");
        exception2.setStatus("OPEN");
        exception2.setModule("BANK_VALIDATION");
        exception2.setTimestamp(LocalDateTime.now());
        
        List<PaymentException> exceptions = Arrays.asList(exception1, exception2);
        
        List<String> recipients = Arrays.asList("support@example.com", "it@example.com");

        // When
        notificationService.sendExceptionReport(exceptions, recipients);

        // Then - Verify the appropriate email service method is called
        // Note: This might need adjustment based on your actual implementation
        verify(emailService, times(1)).sendEmail(any(VendorPayment.class));
    }





}
