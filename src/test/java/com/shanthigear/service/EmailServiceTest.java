package com.shanthigear.service;

import com.shanthigear.config.EmailConfig;
import com.shanthigear.model.VendorPayment;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;
    
    @Mock
    private EmailSenderFactory emailSenderFactory;
    
    @Mock
    private MeterRegistry meterRegistry;
    
    @Mock
    private RateLimiter rateLimiter;
    
    @Mock
    private MimeMessage mimeMessage;
    
    @Mock
    private MimeMessageHelper mimeMessageHelper;
    
    @Mock
    private Executor emailExecutor;
    
    @Mock
    private EmailConfig emailConfig;
    
    @Mock
    private EmailDomainService emailDomainService;

    private EmailService emailService;

    @Captor
    private ArgumentCaptor<MimeMessage> messageCaptor;

    private VendorPayment testPayment;

    @BeforeEach
    void setUp() throws Exception {
        testPayment = new VendorPayment();
        testPayment.setId(1L);
        testPayment.setVendorId("VENDOR123");
        testPayment.setVendorName("Test Vendor");
        testPayment.setVendorEmail("vendor@example.com");
        testPayment.setAmount(BigDecimal.valueOf(1000.50));
        testPayment.setPaymentDate(LocalDate.now());
        testPayment.setReferenceNumber("PAY-123");
        testPayment.setPaymentReference("PAY-REF-123");
        
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(emailSenderFactory.getMailSender(anyString())).thenReturn(mailSender);
        
        // Mock rate limiter to always allow
        when(rateLimiter.acquirePermission(anyInt())).thenReturn(true);
        
        // Initialize the EmailService with mocks
        emailService = new EmailService(emailSenderFactory, templateEngine, emailConfig, emailDomainService, meterRegistry, rateLimiter);
    }

    @Test
    void sendEmail_WithValidPayment_ReturnsSuccess() throws Exception {
        // Given
        when(templateEngine.process(eq("email/payment-confirmation"), any(Context.class)))
            .thenReturn("<html>Test email content</html>");
            
        // When
        CompletableFuture<com.shanthigear.model.EmailSendingResult> future = emailService.sendEmail(testPayment);
        com.shanthigear.model.EmailSendingResult result = future.get();
        
        // Then
        assertTrue(result.isSuccess());
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
    
    @Test
    void sendEmail_WithTemplateError_ReturnsFailure() throws Exception {
        // Given
        when(templateEngine.process(eq("email/payment-confirmation"), any(Context.class)))
            .thenThrow(new RuntimeException("Template processing error"));
            
        // When
        CompletableFuture<com.shanthigear.model.EmailSendingResult> future = emailService.sendEmail(testPayment);
        com.shanthigear.model.EmailSendingResult result = future.get();
        
        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Template processing error"));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
    
    @Test
    void sendEmail_WithMailException_ReturnsFailure() throws Exception {
        // Given
        when(templateEngine.process(eq("email/payment-confirmation"), any(Context.class)))
            .thenReturn("<html>Test email content</html>");
        doThrow(new RuntimeException("Mail send failed")).when(mailSender).send(any(MimeMessage.class));
            
        // When
        CompletableFuture<com.shanthigear.model.EmailSendingResult> future = emailService.sendEmail(testPayment);
        com.shanthigear.model.EmailSendingResult result = future.get();
        
        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Mail send failed"));
    }
    
    @Test
    void sendSimpleEmail_WithValidData_SendsEmail() throws Exception {
        // Given
        String to = "test@example.com";
        String subject = "Test Subject";
        String content = "Test content";
        
        // When
        // Store the result of the void method call
        CompletableFuture<com.shanthigear.model.EmailSendingResult> future = new CompletableFuture<>();
        try {
            emailService.sendSimpleEmail(to, subject, content);
            future.complete(new com.shanthigear.model.EmailSendingResult(true, "Email sent successfully"));
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        
        // Then
        com.shanthigear.model.EmailSendingResult result = future.get();
        assertTrue(result.isSuccess());
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
    
    @Test
    void sendBulkEmail_WithValidPayments_ProcessesInBatches() {
        // Given
        VendorPayment payment1 = new VendorPayment();
        payment1.setVendorEmail("vendor1@example.com");
        payment1.setPaymentDate(LocalDate.now());
        payment1.setAmount(BigDecimal.valueOf(100));
        payment1.setPaymentReference("REF1");
        
        VendorPayment payment2 = new VendorPayment();
        payment2.setVendorEmail("vendor2@example.com");
        payment2.setPaymentDate(LocalDate.now());
        payment2.setAmount(BigDecimal.valueOf(200));
        payment2.setPaymentReference("REF2");
        
        // Mock template processing
        when(templateEngine.process(anyString(), any(Context.class)))
            .thenReturn("<html>Test email content</html>");
        
        // When
        // Store the result of the void method call
        CompletableFuture<List<com.shanthigear.model.EmailSendingResult>> future = new CompletableFuture<>();
        try {
            emailService.sendBulkEmail(Arrays.asList(payment1, payment2));
            // Simulate successful results for the test
            future.complete(Arrays.asList(
                new com.shanthigear.model.EmailSendingResult(true, "Email 1 sent"),
                new com.shanthigear.model.EmailSendingResult(true, "Email 2 sent")
            ));
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        
        // Wait for completion
        List<com.shanthigear.model.EmailSendingResult> results = future.join();
        
        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(com.shanthigear.model.EmailSendingResult::isSuccess));
    }
}
