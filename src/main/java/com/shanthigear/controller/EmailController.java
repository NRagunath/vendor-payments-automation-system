package com.shanthigear.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.shanthigear.model.PaymentStatus;
import com.shanthigear.model.Vendor;
import com.shanthigear.model.VendorPayment;
import com.shanthigear.repository.VendorPaymentRepository;
import com.shanthigear.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/email")
public class EmailController {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);

    private final EmailService emailService;
    private final VendorPaymentRepository vendorPaymentRepository;
    
    @Value("${app.email.sender}")
    private String defaultSender;
    
    @Value("${app.email.sender.name}")
    private String senderName;
    
    @Value("${app.email.recipients}")
    private List<String> defaultRecipients;
    
    @Value("${app.email.test.vendor-name:Shanthi Gears Vendor}")
    private String testVendorName;

    @Value("${mail.vendor-email}")
    private String testVendorEmail;

    @Autowired
    public EmailController(EmailService emailService, VendorPaymentRepository vendorPaymentRepository) {
        this.emailService = emailService;
        this.vendorPaymentRepository = vendorPaymentRepository;
    }
    
    @PostMapping("/test-multiple")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendTestEmails(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        // Safely extract emails from request
        List<String> emails = new ArrayList<>();
        try {
            Object emailsObj = request.get("emails");
            if (emailsObj instanceof List) {
                for (Object item : (List<?>) emailsObj) {
                    if (item instanceof String) {
                        emails.add((String) item);
                    }
                }
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Invalid email list format");
            return ResponseEntity.badRequest().body(response);
        }
        
        // If no emails provided, use default recipients
        if (emails.isEmpty()) {
            if (defaultRecipients != null) {
                emails.addAll(defaultRecipients);
            }
        }
        
        if (emails.isEmpty()) {
            response.put("status", "error");
            response.put("message", "No valid email addresses provided and no default recipients configured");
            return ResponseEntity.badRequest().body(response);
        }
        
        List<String> success = new ArrayList<>();
        List<Map<String, String>> errors = new ArrayList<>();
        
        for (String email : emails) {
            sendTestEmail(email, success, errors);
        }
        
        response.put("status", "success");
        response.put("successfulEmails", success);
        response.put("failedEmails", errors);
        response.put("successCount", success.size());
        response.put("errorCount", errors.size());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/test-all")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendTestToAll() {
        Map<String, Object> response = new HashMap<>();
        
        if (defaultRecipients == null || defaultRecipients.isEmpty()) {
            response.put("status", "error");
            response.put("message", "No default recipients configured");
            return ResponseEntity.badRequest().body(response);
        }
        
        List<String> successList = new ArrayList<>();
        List<Map<String, String>> errorList = new ArrayList<>();
        
        for (String email : defaultRecipients) {
            sendTestEmail(email, successList, errorList);
        }
        
        response.put("status", "success");
        response.put("successfulEmails", successList);
        response.put("failedEmails", errorList);
        response.put("successCount", successList.size());
        response.put("errorCount", errorList.size());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/test-vendor/{vendorId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendVendorPayments(@PathVariable String vendorId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get last 10 payments for the vendor
            Vendor testVendor = Vendor.builder()
                .vendorNumber(vendorId)
                .vendorName(testVendorName)
                .emailAddress("test@example.com")
                .addressLine1("Test Address")
                .build();
                
            List<VendorPayment> payments = vendorPaymentRepository.findTop10ByVendorOrderByPaymentDateDesc(testVendor);
            
            if (payments.isEmpty()) {
                response.put("status", "error");
                response.put("message", "No payments found for vendor: " + vendorId);
                return ResponseEntity.ok(response);
            }
            
            // Get the vendor's email from the first payment
            String vendorEmail = payments.get(0).getVendorEmail();
            if (vendorEmail == null || vendorEmail.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "No email address found for vendor: " + vendorId);
                return ResponseEntity.badRequest().body(response);
            }
            
            // Send bulk email with all payments
            emailService.sendBulkEmail(payments);
            
            response.put("status", "success");
            response.put("message", "Email sent with " + payments.size() + " payments");
            response.put("vendorId", vendorId);
            response.put("vendorEmail", vendorEmail);
            response.put("paymentCount", payments.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to send email: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    private void sendTestEmail(String email, List<String> success, List<Map<String, String>> errors) {
        try {
            // Create test payments (10 sample payments)
            List<VendorPayment> payments = new ArrayList<>();
            LocalDate today = LocalDate.now();
            
            for (int i = 1; i <= 10; i++) {
                VendorPayment payment = new VendorPayment();
                payment.setVendorName(testVendorName);
                payment.setVendorEmail(email);
                payment.setAmount(new BigDecimal(1000 + (i * 100)));
                payment.setPaymentDate(today.minusDays(i));
                payment.setPaymentReference("REF-" + (1000 + i));
                payment.setInvoiceNumber("INV-2023-00" + i);
                payment.setStatus(PaymentStatus.PENDING); // Changed from COMPLETED to PENDING
                payment.setTransactionId("TXN" + System.currentTimeMillis() + i);
                payment.setNotificationSent(false);
                payment.setBankAccount("1234567890");
                payment.setIfscCode("HDFC0001234");
                payment.setDescription("Test Payment " + i);
                
                // Set timestamps
                LocalDateTime now = LocalDateTime.now();
                payment.setCreatedAt(now);
                payment.setUpdatedAt(now);
                
                payments.add(payment);
            }
            
            // Send bulk email with test payments
            emailService.sendBulkEmail(payments);
            success.add(email);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("email", email);
            error.put("error", e.getMessage());
            errors.add(error);
        }
    }
    
    /**
     * Test endpoint to send a payment notification with sample data
     */
    /**
     * Helper method to create a test payment with common fields
     */
    private VendorPayment createTestPayment(Vendor vendor, LocalDate paymentDate, BigDecimal amount, 
                                          String invoiceNumber, String statusStr) {
        VendorPayment payment = new VendorPayment();
        payment.setVendor(vendor);
        String vendorId = vendor.getVendorNumber();
        String vendorName = vendor.getVendorName();
        String email = vendor.getEmailAddress();
        payment.setVendorId(vendorId);
        payment.setVendorName(vendorName);
        payment.setVendorEmail(email);
        payment.setAmount(amount);
        payment.setPaymentDate(paymentDate);
        payment.setPaymentReference("SGL-REF-" + invoiceNumber);
        payment.setInvoiceNumber(invoiceNumber);
        payment.setStatus(PaymentStatus.valueOf(statusStr));
        payment.setTransactionId("TXN" + System.currentTimeMillis() + "-" + invoiceNumber);
        payment.setNotificationSent(false);
        payment.setBankAccount("9876543210");
        payment.setIfscCode("HDFC0009876");
        payment.setDescription("Payment for " + invoiceNumber);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        return payment;
    }
    
    @GetMapping("/test-payment-notification")
    public ResponseEntity<Map<String, Object>> testPaymentNotification() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Create test vendor
            Vendor vendor = Vendor.builder()
                .vendorName(testVendorName)
                .emailAddress(testVendorEmail)
                .vendorNumber("VENDOR-001")
                .addressLine1("123 Industrial Area")
                .city("Coimbatore")
                .state("Tamil Nadu")
                .pincode("641062")
                .build();
            
            // Create test payments with TDS entries
            List<VendorPayment> payments = new ArrayList<>();
            LocalDate today = LocalDate.now();
            
            // Add sample payments with TDS entries
            // First payment with TDS
            VendorPayment payment1 = createTestPayment(vendor, today.minusDays(1), 
                new BigDecimal("105000"), "INV-2024-001", "COMPLETED");
            payment1.setInvoiceDate(today.minusDays(31));
            payments.add(payment1);
            
            // Add TDS entry for first payment (2% TDS)
            VendorPayment tds1 = createTestPayment(vendor, today.minusDays(1), 
                new BigDecimal("-2100"), "INV-2024-001-TDS-01", "COMPLETED");
            tds1.setInvoiceDate(today.minusDays(31));
            tds1.setDescription("TDS Deduction for INV-2024-001 @ 2%");
            payments.add(tds1);
            
            // Second payment with TDS
            VendorPayment payment2 = createTestPayment(vendor, today.minusDays(2), 
                new BigDecimal("110000"), "INV-2024-002", "COMPLETED");
            payment2.setInvoiceDate(today.minusDays(32));
            payments.add(payment2);
            
            // Add TDS entry for second payment (2% TDS)
            VendorPayment tds2 = createTestPayment(vendor, today.minusDays(2), 
                new BigDecimal("-2200"), "INV-2024-002-TDS-01", "COMPLETED");
            tds2.setInvoiceDate(today.minusDays(32));
            tds2.setDescription("TDS Deduction for INV-2024-002 @ 2%");
            payments.add(tds2);
            
            // Add more regular payments without TDS for variety
            VendorPayment payment3 = createTestPayment(vendor, today.minusDays(3), 
                new BigDecimal("115000"), "INV-2024-003", "COMPLETED");
            payment3.setInvoiceDate(today.minusDays(33));
            payments.add(payment3);
            
            VendorPayment payment4 = createTestPayment(vendor, today.minusDays(4), 
                new BigDecimal("120000"), "INV-2024-004", "COMPLETED");
            payment4.setInvoiceDate(today.minusDays(34));
            payments.add(payment4);
            
            VendorPayment payment5 = createTestPayment(vendor, today.minusDays(5), 
                new BigDecimal("125000"), "INV-2024-005", "PENDING");
            payment5.setInvoiceDate(today.minusDays(35));
            payments.add(payment5);
            
            VendorPayment payment6 = createTestPayment(vendor, today.minusDays(6), 
                new BigDecimal("130000"), "INV-2024-006", "FAILED");
            payment6.setInvoiceDate(today.minusDays(36));
            payments.add(payment6);
            
            // Send email with the test payments
            emailService.sendBulkEmail(payments);
            
            // Prepare response
            List<Map<String, Object>> paymentDetails = payments.stream()
                .map(p -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("invoiceNumber", p.getInvoiceNumber());
                    detail.put("amount", p.getAmount());
                    detail.put("status", p.getStatus());
                    detail.put("paymentDate", p.getPaymentDate());
                    detail.put("invoiceDate", p.getInvoiceDate());
                    detail.put("description", p.getDescription());
                    return detail;
                })
                .collect(Collectors.toList());
            
            response.put("status", "success");
            response.put("message", "Test payment notification sent successfully");
            response.put("vendor", testVendorName);
            response.put("email", testVendorEmail);
            response.put("paymentCount", payments.size());
            response.put("payments", paymentDetails);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error sending test payment notification: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Failed to send test payment notification: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
