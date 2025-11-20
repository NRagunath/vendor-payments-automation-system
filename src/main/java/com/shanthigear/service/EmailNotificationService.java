package com.shanthigear.service;

import com.shanthigear.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Email implementation of the NotificationService for sending payment-related emails.
 * Uses the enhanced EmailService which supports multiple email domains.
 */
@Service
public class EmailNotificationService implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
    
    @Value("${app.company.name:Shanthi Gears Limited}")
    private String companyName;
    
    @Value("${app.email.default-currency:INR}")
    private String defaultCurrency;
    
    @Value("${app.email.internal-recipients:}")
    private List<String> internalRecipients;
    
    @Value("${app.internal.recipients.finance:finance-team@shanthigears.com}")
    private String financeTeamEmail;
    
    @Value("${app.internal.recipients.operations:operations@shanthigears.com}")
    private String operationsTeamEmail;
    
    @Value("${app.internal.recipients.it:it-support@shanthigears.com}")
    private String itSupportEmail;
    
    @Value("${app.internal.recipients.management:management@shanthigears.com}")
    private String managementEmail;
    
    private final EmailService emailService;

    @Autowired
    public EmailNotificationService(EmailService emailService) {
        this.emailService = emailService;
    }
    
    /**
     * Helper method to send an email using the new EmailService.
     * @param recipients List of recipient email addresses
     * @param subject Email subject
     * @param templateName Name of the email template (without extension)
     * @param templateVars Template variables
     */
    private CompletableFuture<Void> sendEmail(List<String> recipients, String subject, 
                                            String templateName, Map<String, Object> templateVars) {
        if (recipients == null || recipients.isEmpty() || subject == null || templateName == null) {
            logger.warn("Cannot send email - missing required parameters");
            return CompletableFuture.completedFuture(null);
        }
        
        // Add common template variables
        if (templateVars == null) {
            templateVars = new HashMap<>();
        }
        templateVars.putIfAbsent("companyName", companyName);
        templateVars.putIfAbsent("currentYear", String.valueOf(java.time.Year.now().getValue()));
        
        // Send to all recipients in parallel
        return emailService.sendBulkEmail(recipients, subject, templateName, templateVars)
                .exceptionally(ex -> {
                    logger.error("Failed to send email to {} recipients: {}", recipients.size(), ex.getMessage(), ex);
                    return null;
                });
    }
    
    /**
     * Helper method to send an email to a single recipient.
     * @param to Recipient email address
     * @param subject Email subject
     * @param templateName Name of the email template (without extension)
     * @param templateVars Template variables
     */
    private CompletableFuture<Void> sendEmail(String to, String subject, 
                                            String templateName, Map<String, Object> templateVars) {
        if (to == null || to.trim().isEmpty()) {
            logger.warn("Cannot send email - no recipient specified");
            return CompletableFuture.completedFuture(null);
        }
        return sendEmail(Collections.singletonList(to), subject, templateName, templateVars);
    }

    @Override
    @Async
    public void sendPaymentNotification(Vendor vendor, VendorPayment payment) {
        if (vendor == null || payment == null) {
            logger.warn("Cannot send payment notification - vendor or payment is null");
            return;
        }
        
        String recipientEmail = vendor.getEmailAddress();
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            logger.warn("No email address provided for vendor: {}", vendor.getVendorNumber());
            return;
        }
        
        String subject = String.format("Payment Credited - %s (Ref: %s)", 
            companyName, 
            payment.getPaymentReference());
        
        // Build template variables for Thymeleaf
        Map<String, Object> templateVars = new HashMap<>();
        templateVars.put("vendorName", vendor.getVendorName());
        templateVars.put("paymentReference", payment.getPaymentReference());
        templateVars.put("utrNumber", payment.getReferenceNumber() != null ? payment.getReferenceNumber() : "N/A");
        templateVars.put("invoiceNumber", payment.getInvoiceNumber() != null ? payment.getInvoiceNumber() : "N/A");
        templateVars.put("amount", formatCurrency(payment.getAmount()));
        templateVars.put("currency", defaultCurrency);
        templateVars.put("paymentDate", payment.getCreatedAt() != null ? 
            payment.getCreatedAt().format(DATE_FORMATTER) : LocalDateTime.now().format(DATE_FORMATTER));
        templateVars.put("bankAccount", payment.getBankAccount() != null ? payment.getBankAccount() : "N/A");
        templateVars.put("ifscCode", payment.getIfscCode() != null ? payment.getIfscCode() : "N/A");
        
        // Send email using the enhanced EmailService
        sendEmail(recipientEmail, subject, "payment-notification", templateVars)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send payment notification email for payment reference: {}", 
                        payment.getPaymentReference(), ex);
                } else {
                    logger.info("Payment notification email sent to {} for payment reference: {}", 
                        recipientEmail, payment.getPaymentReference());
                }
            });
    }
    
    @Override
    @Async
    public void sendPaymentConfirmation(VendorPayment payment) {
        if (payment == null || payment.getVendorEmail() == null) {
            logger.warn("Cannot send payment confirmation - payment or vendor email is null");
            return;
        }
        
        // Build template variables for Thymeleaf
        Map<String, Object> templateVars = new HashMap<>();
        templateVars.put("vendorName", payment.getVendorName());
        templateVars.put("paymentReference", payment.getPaymentReference());
        templateVars.put("utrNumber", payment.getReferenceNumber() != null ? payment.getReferenceNumber() : "N/A");
        templateVars.put("invoiceNumber", payment.getInvoiceNumber() != null ? payment.getInvoiceNumber() : "N/A");
        templateVars.put("amount", formatCurrency(payment.getAmount()));
        templateVars.put("currency", defaultCurrency);
        templateVars.put("paymentDate", payment.getPaymentDate() != null ? 
            payment.getPaymentDate().format(DATE_FORMATTER) : LocalDateTime.now().format(DATE_FORMATTER));
        templateVars.put("status", payment.getStatus() != null ? payment.getStatus().name() : "COMPLETED");
        templateVars.put("bankAccount", payment.getBankAccount() != null ? payment.getBankAccount() : "N/A");
        templateVars.put("ifscCode", payment.getIfscCode() != null ? payment.getIfscCode() : "N/A");
        
        String subject = String.format("Payment Confirmation - %s", payment.getPaymentReference());
        
        // Send email using the enhanced EmailService
        sendEmail(payment.getVendorEmail(), subject, "payment-confirmation", templateVars)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send payment confirmation email to {}: {}", 
                        payment.getVendorEmail(), ex.getMessage(), ex);
                } else {
                    logger.info("Payment confirmation email sent to {}", payment.getVendorEmail());
                }
            });
    }

    @Override
    @Async
    public void sendPaymentFailure(VendorPayment payment, String reason) {
        if (payment == null || payment.getVendorEmail() == null) {
            logger.warn("Cannot send payment failure notification - payment or vendor email is null");
            return;
        }
        
        // Build template variables for Thymeleaf
        Map<String, Object> templateVars = new HashMap<>();
        templateVars.put("vendorName", payment.getVendorName());
        templateVars.put("paymentReference", payment.getPaymentReference());
        templateVars.put("failureReason", reason);
        templateVars.put("amount", payment.getAmount() != null ? formatCurrency(payment.getAmount()) : "N/A");
        templateVars.put("paymentDate", payment.getPaymentDate() != null ? 
            payment.getPaymentDate().format(DATE_FORMATTER) : LocalDateTime.now().format(DATE_FORMATTER));
        templateVars.put("supportEmail", itSupportEmail);
        
        String subject = String.format("Payment Failed - %s", payment.getPaymentReference());
        
        // Send email using the enhanced EmailService
        sendEmail(payment.getVendorEmail(), subject, "payment-failure", templateVars)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send payment failure notification to {}: {}", 
                        payment.getVendorEmail(), ex.getMessage(), ex);
                } else {
                    logger.info("Payment failure notification sent to {}", payment.getVendorEmail());
                    
                    // Also notify internal team about the failure
                    Map<String, Object> internalVars = new HashMap<>();
                    internalVars.put("subject", subject);
                    internalVars.put("vendorName", payment.getVendorName());
                    internalVars.put("paymentReference", payment.getPaymentReference());
                    internalVars.put("failureReason", reason);
                    internalVars.put("amount", formatCurrency(payment.getAmount()));
                    internalVars.put("paymentDate", LocalDateTime.now().format(DATE_FORMATTER));
                    
                    sendEmail(
                        Collections.singletonList(itSupportEmail),
                        "[Internal] " + subject,
                        "internal/payment-failure-notification",
                        internalVars
                    );
                }
            });
    }

    @Override
    @Async
    public void sendOverduePaymentNotification(VendorPayment payment) {
        if (payment == null) {
            logger.warn("Cannot send overdue notification: payment is null");
            return;
        }
        
        if (payment.getVendorEmail() == null) {
            logger.warn("Cannot send overdue notification - vendor email is null for payment {}", 
                payment.getPaymentReference());
            return;
        }
        
        if (payment.getDueDate() == null) {
            logger.warn("Cannot send overdue notification - due date is null for payment {}", 
                payment.getPaymentReference());
            return;
        }

        // Calculate days overdue
        LocalDate dueDate = payment.getDueDate();
        if (dueDate == null) {
            logger.warn("Cannot calculate days overdue - due date is null for payment {}", 
                payment.getPaymentReference());
            dueDate = LocalDate.now(); // Default to today if due date is not set
        }
        // Calculate and ensure daysOverdue is not negative
        final long daysOverdue = Math.max(0, ChronoUnit.DAYS.between(dueDate, LocalDate.now()));
        
        // Build template variables for Thymeleaf
        Map<String, Object> templateVars = new HashMap<>();
        templateVars.put("vendorName", payment.getVendorName());
        templateVars.put("paymentReference", payment.getPaymentReference());
        templateVars.put("invoiceNumber", payment.getInvoiceNumber() != null ? payment.getInvoiceNumber() : "N/A");
        templateVars.put("dueDate", payment.getDueDate().format(DateTimeFormatter.ISO_DATE));
        templateVars.put("daysOverdue", daysOverdue);
        templateVars.put("amount", formatCurrency(payment.getAmount()));
        templateVars.put("currency", defaultCurrency);
        templateVars.put("financeEmail", financeTeamEmail);
        
        String subject = String.format("Overdue Payment - %s", payment.getPaymentReference());
        
        // Send email using the enhanced EmailService
        sendEmail(payment.getVendorEmail(), subject, "overdue-payment", templateVars)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send overdue payment notification to {}: {}", 
                        payment.getVendorEmail(), ex.getMessage(), ex);
                } else {
                    logger.info("Overdue payment notification sent to {}", payment.getVendorEmail());
                    
                    // Also notify internal team
                    Map<String, Object> internalVars = new HashMap<>();
                    internalVars.put("subject", subject);
                    internalVars.put("vendorName", payment.getVendorName());
                    internalVars.put("paymentReference", payment.getPaymentReference());
                    internalVars.put("daysOverdue", daysOverdue);
                    internalVars.put("amount", formatCurrency(payment.getAmount()));
                    internalVars.put("dueDate", payment.getDueDate().format(DateTimeFormatter.ISO_DATE));
                    internalVars.put("financeEmail", financeTeamEmail);
                    
                    sendEmail(
                        Collections.singletonList(financeTeamEmail),
                        "[Internal] " + subject,
                        "internal/overdue-payment-notification",
                        internalVars
                    );
                }
            });
    }
    
    /**
     * Formats a BigDecimal amount as a currency string.
     * @param amount The amount to format
     * @return Formatted currency string
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "N/A";
        }
        return NumberFormat.getCurrencyInstance(Locale.of("en", "IN")).format(amount);
    }
    
    @Override
    @Async
    public void sendPaymentSummary(PaymentSummary summary, List<String> recipients) {
        if (summary == null || recipients == null || recipients.isEmpty()) {
            logger.warn("Cannot send payment summary - summary is null or no recipients specified");
            return;
        }
        
        // Filter out any null or empty email addresses
        List<String> validRecipients = recipients.stream()
            .filter(email -> email != null && !email.trim().isEmpty())
            .distinct()
            .collect(Collectors.toList());
            
        if (validRecipients.isEmpty()) {
            logger.warn("No valid email recipients provided for payment summary");
            return;
        }
        
        // Prepare template variables for Thymeleaf
        Map<String, Object> templateVars = new HashMap<>();
        templateVars.put("summary", summary);
        templateVars.put("companyName", companyName);
        templateVars.put("generatedAt", LocalDateTime.now().format(DATE_FORMATTER));
        templateVars.put("currency", defaultCurrency);
        templateVars.put("financeEmail", financeTeamEmail);
        
        String subject = String.format("Payment Summary Report - %s - %s", 
            summary.getBatchId(), 
            summary.getPaymentDate() != null ? summary.getPaymentDate().format(DateTimeFormatter.ISO_DATE) : "N/A");
        
        // Send email to all recipients using the new email service
        sendEmail(validRecipients, subject, "internal/payment-summary", templateVars)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send payment summary to some recipients: {}", ex.getMessage(), ex);
                } else {
                    logger.info("Payment summary sent to {} recipients", validRecipients.size());
                }
            });
    }
    
    @Override
    @Async
    public void sendExceptionReport(List<PaymentException> exceptions, List<String> recipients) {
        if (exceptions == null || exceptions.isEmpty() || recipients == null || recipients.isEmpty()) {
            logger.warn("Cannot send exception report - no exceptions or recipients specified");
            return;
        }
        
        // Check for critical issues and add IT support if needed
        boolean hasCriticalIssues = exceptions.stream()
            .anyMatch(e -> "CRITICAL".equalsIgnoreCase(e.getSeverity()));
            
        // Filter out any null or empty email addresses
        Set<String> allRecipients = recipients.stream()
            .filter(email -> email != null && !email.trim().isEmpty())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        
        // Add IT support for critical issues
        if (hasCriticalIssues && itSupportEmail != null && !itSupportEmail.trim().isEmpty()) {
            allRecipients.add(itSupportEmail);
        }
        
        if (allRecipients.isEmpty()) {
            logger.warn("No valid email recipients provided for exception report");
            return;
        }
        
        // Group exceptions by severity for reporting
        Map<String, Long> severityCounts = exceptions.stream()
            .collect(Collectors.groupingBy(
                e -> e.getSeverity() != null ? e.getSeverity() : "UNKNOWN", 
                Collectors.counting()
            ));
        
        // Prepare template variables for Thymeleaf
        Map<String, Object> templateVars = new HashMap<>();
        templateVars.put("exceptions", exceptions);
        templateVars.put("totalExceptions", exceptions.size());
        templateVars.put("severityCounts", severityCounts);
        templateVars.put("companyName", companyName);
        templateVars.put("reportDate", LocalDateTime.now().format(DATE_FORMATTER));
        templateVars.put("hasCriticalIssues", hasCriticalIssues);
        templateVars.put("itSupportEmail", itSupportEmail);
        
        String subject = String.format("Payment Exception Report - %d Issue%s Found", 
            exceptions.size(),
            exceptions.size() != 1 ? "s" : "");
        
        if (hasCriticalIssues) {
            subject = "[CRITICAL] " + subject;
        }
        
        // Send email to all recipients using the new email service
        sendEmail(new ArrayList<>(allRecipients), subject, "internal/exception-report", templateVars)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send exception report to some recipients: {}", ex.getMessage(), ex);
                } else {
                    logger.info("Exception report sent to {} recipients with {} exceptions", 
                        allRecipients.size(), exceptions.size());
                }
            });
    }
    
    @Override
    @Async
    public void sendActionItems(List<ActionItem> actionItems, List<String> recipients) {
        if (actionItems == null || actionItems.isEmpty() || recipients == null || recipients.isEmpty()) {
            logger.warn("Cannot send action items - no items or recipients specified");
            return;
        }
        
        // Filter out any null or empty email addresses
        List<String> validRecipients = recipients.stream()
            .filter(email -> email != null && !email.trim().isEmpty())
            .distinct()
            .collect(Collectors.toList());
            
        if (validRecipients.isEmpty()) {
            logger.warn("No valid email recipients provided for action items");
            return;
        }
        
        // Group action items by assignee
        Map<String, List<ActionItem>> itemsByAssignee = actionItems.stream()
            .filter(ai -> ai.getAssignedTo() != null && !ai.getAssignedTo().trim().isEmpty())
            .collect(Collectors.groupingBy(ActionItem::getAssignedTo));
        
        // Check for high priority items
        boolean hasHighPriority = actionItems.stream()
            .anyMatch(ai -> "HIGH".equalsIgnoreCase(ai.getPriority()));
        
        // Prepare common template variables for Thymeleaf
        Map<String, Object> commonVars = new HashMap<>();
        commonVars.put("actionItems", actionItems);
        commonVars.put("itemsByAssignee", itemsByAssignee);
        commonVars.put("totalItems", actionItems.size());
        commonVars.put("companyName", companyName);
        commonVars.put("currentDate", LocalDateTime.now().format(DATE_FORMATTER));
        commonVars.put("hasHighPriority", hasHighPriority);
        commonVars.put("itSupportEmail", itSupportEmail);
        
        // Prepare subject with priority indicator
        String subject = String.format("%sAction Items - %d Task%s Assigned",
            hasHighPriority ? "[URGENT] " : "",
            actionItems.size(),
            actionItems.size() != 1 ? "s" : "");
        
        // Send personalized emails to each recipient
        List<CompletableFuture<Void>> emailFutures = new ArrayList<>();
        
        for (String recipient : validRecipients) {
            // Create a copy of the common variables for this recipient
            Map<String, Object> recipientVars = new HashMap<>(commonVars);
            
                // Get items assigned to this recipient
            List<ActionItem> recipientItems = itemsByAssignee.getOrDefault(recipient, Collections.emptyList());
            recipientVars.put("recipientItems", recipientItems);
            
            // Send the email
            CompletableFuture<Void> emailFuture = sendEmail(
                recipient,
                subject,
                "internal/action-items",
                recipientVars
            );
            
            emailFuture.whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send action items to {}: {}", recipient, ex.getMessage());
                } else {
                    logger.debug("Action items sent to {} ({} items)", recipient, recipientItems.size());
                }
            });
            
            emailFutures.add(emailFuture);
        }
        
        // Log completion when all emails are sent
        CompletableFuture.allOf(emailFutures.toArray(new CompletableFuture<?>[0]))
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send some action item emails: {}", ex.getMessage(), ex);
                } else {
                    logger.info("Action items sent to {} recipients with {} total items", 
                        validRecipients.size(), actionItems.size());
                }
            });
    }
    
    @Override
    @Async
    public void sendDailySummary(PaymentSummary summary, List<String> recipients) {
        if (summary == null || recipients == null || recipients.isEmpty()) {
            logger.warn("Cannot send daily summary - summary is null or no recipients specified");
            return;
        }
        
        // Filter out any null or empty email addresses
        List<String> validRecipients = recipients.stream()
            .filter(email -> email != null && !email.trim().isEmpty())
            .distinct()
            .collect(Collectors.toList());
            
        if (validRecipients.isEmpty()) {
            logger.warn("No valid email recipients provided for daily summary");
            return;
        }
        
        // Prepare template variables for Thymeleaf
        Map<String, Object> templateVars = new HashMap<>();
        templateVars.put("summary", summary);
        templateVars.put("companyName", companyName);
        templateVars.put("reportDate", LocalDateTime.now().format(DATE_FORMATTER));
        templateVars.put("currency", defaultCurrency);
        templateVars.put("financeEmail", financeTeamEmail);
        
        String subject = String.format("Daily Payment Summary - %s", 
            LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        
        // Send email to all recipients using the new email service
        sendEmail(validRecipients, subject, "internal/daily-summary", templateVars)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send daily summary to some recipients: {}", ex.getMessage(), ex);
                } else {
                    logger.info("Daily summary sent to {} recipients", validRecipients.size());
                }
            });
    }
}
