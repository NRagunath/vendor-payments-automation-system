package com.shanthigear.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.shanthigear.model.Vendor;
import com.shanthigear.model.VendorPayment;
import com.shanthigear.model.PaymentStatus;
import com.shanthigear.model.EmailSendingResult;
import com.shanthigear.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@ActiveProfiles("test")
class EmailServiceIntegrationTest {

    private GreenMail greenMail;

    @Autowired
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        // Start GreenMail server on a random port
        greenMail = new GreenMail(new ServerSetup(0, null, "smtp"));
        greenMail.start();
        
        // Configure system properties for GreenMail
        System.setProperty("spring.mail.host", "localhost");
        System.setProperty("spring.mail.port", String.valueOf(greenMail.getSmtp().getPort()));
    }

    @AfterEach
    void tearDown() {
        greenMail.stop();
    }

    @Test
    void sendEmail_WithValidVendorPayment_SendsEmail() throws Exception {
        // Given
        Vendor vendor = Vendor.builder()
            .vendorNumber("VENDOR001")
            .vendorName("Test Vendor")
            .emailAddress("recipient@test.com")
            .build();
        
        VendorPayment payment = VendorPayment.builder()
            .id(1L)
            .vendor(vendor)
            .vendorId("VENDOR001")
            .vendorName("Test Vendor")
            .vendorEmail("recipient@test.com")
            .amount(new BigDecimal("1000.50"))
            .paymentDate(java.time.LocalDate.now())
            .referenceNumber("PAY-123")
            .paymentReference("INV-2025-001")
            .status(com.shanthigear.model.PaymentStatus.COMPLETED)
            .build();

        // When
        CompletableFuture<EmailSendingResult> future = emailService.sendEmail(payment);
        
        // Wait for the future to complete with a timeout
        EmailSendingResult result = future.get(10, TimeUnit.SECONDS);
        
        // Give some time for the email to be processed
        assertTrue(greenMail.waitForIncomingEmail(5000, 1));
        
        // Then
        assertTrue(result.isSuccess(), "Email should be sent successfully");
        
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertEquals(1, receivedMessages.length, "Should have received exactly one email");
        
        MimeMessage message = receivedMessages[0];
        assertEquals("recipient@test.com", message.getAllRecipients()[0].toString());
        
        // Check if either subject contains expected text
        String subject = message.getSubject();
        assertTrue(subject.contains("Payment") || subject.contains("PAY-123"), 
                  "Subject should contain 'Payment' or 'PAY-123' but was: " + subject);
    }

    @Test
    void sendEmail_WithInvalidEmail_ReturnsError() throws Exception {
        // Given
        Vendor vendor = Vendor.builder()
            .vendorNumber("VENDOR001")
            .vendorName("Test Vendor")
            .emailAddress("") // Invalid email
            .build();
        
        VendorPayment payment = VendorPayment.builder()
            .id(1L)
            .vendor(vendor)
            .vendorId("VENDOR001")
            .vendorName("Test Vendor")
            .vendorEmail("") // Invalid email
            .amount(new BigDecimal("1000.50"))
            .paymentDate(java.time.LocalDate.now())
            .referenceNumber("PAY-123")
            .paymentReference("INV-2025-001")
            .status(PaymentStatus.COMPLETED)
            .build();

        // When
        CompletableFuture<EmailSendingResult> future = emailService.sendEmail(payment);
        
        // Wait for the future to complete with a timeout
        EmailSendingResult result = future.get(5, TimeUnit.SECONDS);
        
        // Give a small delay to ensure no email is being processed asynchronously
        Thread.sleep(500);
        
        // Then
        assertFalse(result.isSuccess(), "Email sending should fail with invalid email");
        assertTrue(result.getMessage().contains("email") || 
                  result.getMessage().contains("missing"),
                  "Error message should mention email issue but was: " + result.getMessage());
        
        assertEquals(0, greenMail.getReceivedMessages().length, "No emails should be sent");
    }
}
