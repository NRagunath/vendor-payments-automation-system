package com.shanthigear.service;

import com.shanthigear.config.EmailConfig;
import com.shanthigear.model.VendorPayment;
import com.shanthigear.model.EmailSendingResult;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Service for sending email notifications with support for multiple domains.
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 3;
    private static final long MAX_RETRY_DELAY_MS = 30000; // 30 seconds maximum delay
    private static final long RETRY_DELAY_MS = 1000;
    
    /**
     * Get the maximum number of retry attempts for sending an email.
     * This method is protected to allow overriding in tests.
     * @return the maximum number of retry attempts
     */
    protected int getMaxRetries() {
        return MAX_RETRIES;
    }
    
    /**
     * Get the delay between retry attempts in milliseconds.
     * This method is protected to allow overriding in tests.
     * @return the delay between retry attempts in milliseconds
     */
    protected long getRetryDelayMs() {
        return RETRY_DELAY_MS;
    }
    private static final int MAX_CONCURRENT_EMAILS = 100;
    private static final int RATE_LIMIT_RETRY_DELAY_MS = 100;
    private static final int MAX_RATE_LIMIT_RETRIES = 10;
    private final RateLimiter rateLimiter;
    
    private final EmailSenderFactory emailSenderFactory;
    private final TemplateEngine templateEngine;
    private final EmailConfig emailConfig;
    // Kept for potential future use
    @SuppressWarnings("unused")
    private final EmailDomainService emailDomainService;
    private final MeterRegistry meterRegistry;
    private final Executor emailExecutor;
    private final Timer emailSendTimer;
    // Kept for potential future use
    @SuppressWarnings("unused")
    private final Timer batchProcessTimer;
    private final Semaphore concurrentEmailSemaphore;

    public EmailService(EmailSenderFactory emailSenderFactory, 
                      TemplateEngine templateEngine,
                      EmailConfig emailConfig,
                      EmailDomainService emailDomainService,
                      MeterRegistry meterRegistry) {
        this(emailSenderFactory, templateEngine, emailConfig, emailDomainService, 
             meterRegistry, createDefaultRateLimiter());
    }
    
    EmailService(EmailSenderFactory emailSenderFactory,
                         TemplateEngine templateEngine,
                         EmailConfig emailConfig,
                         EmailDomainService emailDomainService,
                         MeterRegistry meterRegistry,
                         RateLimiter rateLimiter) {
        this.emailSenderFactory = emailSenderFactory;
        this.templateEngine = templateEngine;
        this.emailConfig = emailConfig;
        this.emailDomainService = emailDomainService;
        this.meterRegistry = meterRegistry;
        this.rateLimiter = rateLimiter;
        
        // Configure executor for email sending
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        this.emailExecutor = new ThreadPoolExecutor(
            corePoolSize,
            corePoolSize * 2,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // Setup metrics
        this.emailSendTimer = Timer.builder("email.send.time")
            .description("Time taken to send individual emails")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
            
        this.batchProcessTimer = Timer.builder("email.batch.process.time")
            .description("Time taken to process email batches")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
            
        this.concurrentEmailSemaphore = new Semaphore(MAX_CONCURRENT_EMAILS, true);
    }

    private static RateLimiter createDefaultRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .limitForPeriod(100)
            .timeoutDuration(Duration.ofMillis(100))
            .build();
        return RateLimiter.of("emailRateLimiter", config);
    }

    @PostConstruct
    public void init() {
        // Register gauges for monitoring after the bean is fully initialized
        meterRegistry.gauge("email.concurrent.sends", this, 
            s -> (double) (MAX_CONCURRENT_EMAILS - s.concurrentEmailSemaphore.availablePermits()));
            
        meterRegistry.gauge("email.rate.limit.available", this, 
            s -> (double) s.rateLimiter.getMetrics().getAvailablePermissions());
    }

    /**
     * Sends an email notification for the given payment.
     *
     * @param payment the payment details
     * @throws IllegalArgumentException if payment is null
     * @throws IllegalStateException if vendor email is missing
     */
    /**
     * Sends a payment confirmation email to the vendor.
     *
     * @param payment the payment details
     * @throws IllegalArgumentException if payment is null or missing required fields
     */
    /**
     * Send an email for a single payment.
     * @param payment The payment details
     * @throws IllegalArgumentException if payment is null
     */
    /**
     * Sends an email for a single payment with retry logic and rate limiting.
     * @param payment The payment details
     * @return CompletableFuture that completes when the email is sent
     */
    @Async
    public CompletableFuture<EmailSendingResult> sendEmail(VendorPayment payment) {
        if (payment == null) {
            return CompletableFuture.completedFuture(
                new EmailSendingResult(false, "Payment cannot be null"));
        }
        
        String email = payment.getVendorEmail();
        if (email == null || email.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                new EmailSendingResult(false, "Vendor email is missing for payment: " + payment.getId()));
        }
        
        // Extract domain for rate limiting
        String domain = email.substring(email.indexOf('@') + 1);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Acquire semaphore to limit concurrency
                if (!concurrentEmailSemaphore.tryAcquire(30, TimeUnit.SECONDS)) {
                    return new EmailSendingResult(false, "Timeout waiting for email sending slot");
                }
                
                try {
                    // Apply rate limiting with retry
                    int retryCount = 0;
                    while (retryCount < MAX_RATE_LIMIT_RETRIES) {
                        if (tryAcquireToken(domain)) {
                            // Successfully got tokens, proceed with sending
                            return sendEmailWithRetry(email, payment);
                        }
                        
                        // Wait before retrying
                        Thread.sleep(RATE_LIMIT_RETRY_DELAY_MS);
                        retryCount++;
                    }
                    
                    return new EmailSendingResult(false, "Rate limit exceeded after " + MAX_RATE_LIMIT_RETRIES + " retries");
                    
                } finally {
                    concurrentEmailSemaphore.release();
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new EmailSendingResult(false, "Email sending was interrupted: " + e.getMessage());
            } catch (Exception e) {
                logger.error("Unexpected error sending email for payment: " + payment.getId(), e);
                return new EmailSendingResult(false, "Unexpected error: " + e.getMessage());
            }
        }, emailExecutor);
    }
    
    /**
     * Sends a payment confirmation email to the vendor with detailed payment information.
     *
     * @param toEmail Recipient email address
     * @param vendorName Name of the vendor
     * @param paymentReference Payment reference number
     * @param amount Payment amount
     * @param currency Payment currency
     */
    /**
     * Send a payment confirmation email.
     * @param toEmail Recipient email address
     * @param vendorName Name of the vendor
     * @param paymentReference Payment reference number
     * @param amount Payment amount
     * @param currency Payment currency
    @Async
    public CompletableFuture<Void> sendPaymentConfirmation(String toEmail, String vendorName, 
                                                         String paymentReference, 
                                                         double amount, String currency) {
        Map<String, Object> templateVars = new HashMap<>();
        templateVars.put("vendorName", vendorName);
        templateVars.put("paymentReference", paymentReference);
        templateVars.put("amount", String.format("%.2f %s", amount, currency));
        templateVars.put("transactionDate", java.time.LocalDate.now().toString());
        return sendEmail(toEmail, String.format("Payment Confirmation - %s", paymentReference), "payment-confirmation", templateVars);
    }

    /**
     * Process a Thymeleaf template with the given context.
     * @param templateName The name of the template (without extension)
     * @param context The template context
     * @return The processed template as a string
     */
    private String processTemplate(String templateName, Context context) {
        try {
            // Add .html suffix if not present and not already a full path
            String templatePath = templateName.endsWith(".html") ? 
                templateName : templateName + ".html";
            return templateEngine.process(templatePath, context);
        } catch (Exception e) {
            logger.error("Error processing template {}: {}", templateName, e.getMessage(), e);
            throw new RuntimeException("Failed to process template: " + templateName, e);
        }
    }
    
    /**
     * Send a bulk email for multiple payments.
     * @param payments List of payments to include in the email
     * @throws IllegalArgumentException if payments is null or empty
     */
    @Async
    public void sendBulkEmail(List<VendorPayment> payments) {
        if (payments == null || payments.isEmpty()) {
            throw new IllegalArgumentException("Payments list cannot be null or empty");
        }
        
        // Group payments by vendor email domain
        Map<String, List<VendorPayment>> paymentsByDomain = payments.stream()
            .filter(payment -> payment != null && payment.getVendorEmail() != null && !payment.getVendorEmail().trim().isEmpty())
            .collect(Collectors.groupingBy(
                payment -> {
                    String email = payment.getVendorEmail().trim();
                    int atIndex = email.lastIndexOf('@');
                    return atIndex > 0 ? email.substring(atIndex + 1).toLowerCase() : "default";
                }
            ));
        
        if (paymentsByDomain.isEmpty()) {
            throw new IllegalStateException("No valid vendor emails found in the payments list");
        }
        
        // Process each domain group
        for (Map.Entry<String, List<VendorPayment>> entry : paymentsByDomain.entrySet()) {
            String domain = entry.getKey();
            List<VendorPayment> domainPayments = entry.getValue();
            VendorPayment firstPayment = domainPayments.get(0);
            String to = firstPayment.getVendorEmail();
            
            try {
                logger.debug("Preparing to send email to: {} (domain: {})", to, domain);
                logger.debug("Number of payments for domain {}: {}", domain, domainPayments.size());
                
                // Get the appropriate mail sender for this domain
                JavaMailSender mailSender = emailSenderFactory.getMailSender(to);
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                // Set email properties
                helper.setTo(to);
                String subject = "SGL Payment Notification - " + firstPayment.getVendorName();
                helper.setSubject(subject);
                
                // Set from address from configuration
                String fromEmail = emailConfig.getDefaultConfig().getFrom();
                String fromName = emailConfig.getDefaultConfig().getFromName();
                if (fromName != null && !fromName.isEmpty()) {
                    helper.setFrom(String.format("%s <%s>", fromName, fromEmail));
                } else {
                    helper.setFrom(fromEmail);
                }
                
                // Calculate total amount for this domain's payments
                BigDecimal totalAmount = domainPayments.stream()
                    .map(VendorPayment::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                logger.debug("Total amount calculated for domain {}: {}", domain, totalAmount);
                
                // Prepare the evaluation context
                Context context = new Context();
                logger.debug("Setting template variables for domain: {}", domain);
                
                // Add payments and basic info
                context.setVariable("payments", domainPayments);
                context.setVariable("totalAmount", totalAmount);
                context.setVariable("payment", firstPayment);
                context.setVariable("vendor", firstPayment.getVendorName());
                
                // Add current date for the template
                context.setVariable("currentDate", new java.util.Date());
                
                // Add sample payment data for the template
                context.setVariable("paymentDate", new java.util.Date());
                context.setVariable("referenceNumber", "PMT" + 
                    new java.text.SimpleDateFormat("yyMMdd").format(new java.util.Date()) + 
                    "REF");
                    
                // Add bank details
                context.setVariable("bankName", "State Bank of India");
                context.setVariable("branchName", "Industrial Finance Branch");
                context.setVariable("accountNumber", "1234567890123");
                context.setVariable("ifscCode", "SBIN0001234");
                context.setVariable("accountType", "Current");
                
                // Add contact information
                context.setVariable("companyName", "Shanthi Gears Limited");
                context.setVariable("companyPhone", "+91 44 1234 5678");
                context.setVariable("companyEmail", "accounts@shanthigears.com");
                
                try {
                    logger.debug("Processing email template for domain: {}", domain);
                    logger.debug("Template variables for domain {}: {}", domain, context.getVariableNames().stream()
                        .map(name -> name + "=" + context.getVariable(name))
                        .collect(Collectors.joining(", ")));
                    
                    // Process the template
                    String templateName = "payment-notification";
                    String htmlContent = processTemplate(templateName, context);
                    logger.debug("Template processed successfully for domain: {}", domain);
                    
                    // Log the email content for testing
                    if (logger.isDebugEnabled()) {
                        logger.debug("=== EMAIL CONTENT FOR {} ===\n{}", domain, htmlContent);
                        logger.debug("=== END EMAIL CONTENT ===");
                    }
                    
                    helper.setText(htmlContent, true);
                    
                    // Send the email
                    logger.debug("Sending email to domain: {}", domain);
                    mailSender.send(message);
                    logger.info("Email sent successfully to {} with {} payments (domain: {})", 
                              to, domainPayments.size(), domain);
                    
                } catch (Exception e) {
                    logger.error("Error processing email template for domain {}: {}", domain, e.getMessage(), e);
                    throw new RuntimeException("Failed to process email template for domain: " + domain, e);
                }
                
            } catch (MessagingException e) {
                String errorMsg = String.format("Failed to send email to %s (domain: %s): %s", 
                    to, domain, e.getMessage());
                logger.error(errorMsg, e);
                throw new RuntimeException(errorMsg, e);
            } catch (Exception e) {
                String errorMsg = String.format("Error processing email for domain %s: %s", 
                    domain, e.getMessage());
                logger.error(errorMsg, e);
                throw new RuntimeException(errorMsg, e);
            }
        }
    }

    /**
     * Sends an email with the given subject and template.
     *
     * @param to Recipient email address
     * @param subject Email subject
     * @param templateName Name of the template (without extension)
     * @param templateVars Template variables
     * @return CompletableFuture that completes when the email is sent
     */
    @Async
    public CompletableFuture<Void> sendEmail(String to, String subject, String templateName, Map<String, Object> templateVars) {
        if (to == null || to.trim().isEmpty()) {
            logger.warn("Cannot send email - no recipient specified");
            return CompletableFuture.completedFuture(null);
        }

        // Extract domain for rate limiting
        String domain = to.contains("@") ? to.substring(to.indexOf('@') + 1) : "default";
        
        return CompletableFuture.runAsync(() -> {
            int retryCount = 0;
            boolean sent = false;
            Exception lastError = null;
            
            while (retryCount < getMaxRetries() && !sent) {
                try {
                    // Try to acquire a token from rate limiter
                    logger.debug("Attempting to acquire rate limiter token (attempt {}/{})", 
                        retryCount + 1, getMaxRetries());
                        
                    if (!tryAcquireToken(domain)) {
                        meterRegistry.counter("email.rate_limit.exceeded").increment();
                        logger.warn("Rate limit exceeded for domain: {} (attempt {}/{})", 
                            domain, retryCount + 1, getMaxRetries());
                        
                        if (retryCount < MAX_RATE_LIMIT_RETRIES - 1) {
                            logger.debug("Retrying after delay (attempt {}/{})", 
                                retryCount + 1, getMaxRetries());
                            Thread.sleep(RATE_LIMIT_RETRY_DELAY_MS);
                            retryCount++;
                            continue;
                        } else {
                            String errorMsg = "Rate limit exceeded after " + MAX_RATE_LIMIT_RETRIES + " retries";
                            logger.error(errorMsg);
                            throw new RuntimeException(errorMsg);
                        }
                    }
                    
                    // Process the template
                    String emailContent;
                    try {
                        Context context = new Context();
                        // Add default template variables
                        context.setVariable("currentYear", java.time.Year.now().getValue());
                        context.setVariable("companyName", emailConfig.getDefaultConfig().getFromName());
                        
                        // Add provided template variables if any
                        if (templateVars != null) {
                            templateVars.forEach(context::setVariable);
                        }
                        
                        // Process template - don't add .html suffix as it's already handled in the template engine
                        emailContent = processTemplate(templateName, context);
                    } catch (Exception e) {
                        meterRegistry.counter("email.error", "type", "template_processing").increment();
                        logger.error("Error processing email template: {}", e.getMessage(), e);
                        throw new RuntimeException("Failed to process email template", e);
                    }
                    
                    // Get the appropriate mail sender for the domain
                    JavaMailSender mailSender = emailSenderFactory.getMailSender(domain);
                    MimeMessage message = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                    
                    // Set email properties
                    helper.setTo(to);
                    helper.setSubject(subject);
                    
                    // Set from address from configuration
                    String fromEmail = emailConfig.getDefaultConfig().getFrom();
                    String fromName = emailConfig.getDefaultConfig().getFromName();
                    if (fromName != null && !fromName.isEmpty()) {
                        helper.setFrom(String.format("%s <%s>", fromName, fromEmail));
                    } else {
                        helper.setFrom(fromEmail);
                    }
                    
                    // Set email content
                    helper.setText(emailContent, true);
                    
                    // Record metrics for the email send operation
                    Timer.Sample timer = Timer.start(meterRegistry);
                    try {
                        mailSender.send(message);
                        timer.stop(meterRegistry.timer("email.send.time", "status", "success"));
                        meterRegistry.counter("email.sent", "status", "success").increment();
                        sent = true;
                        logger.info("Email sent successfully to {}", to);
                    } catch (Exception e) {
                        timer.stop(meterRegistry.timer("email.send.time", "status", "error"));
                        meterRegistry.counter("email.sent", "status", "error").increment();
                        throw e; // Let the outer catch handle the retry logic
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Email sending was interrupted", e);
                } catch (Exception e) {
                    lastError = e;
                    if (retryCount < getMaxRetries() - 1) {
                        long delay = getRetryDelayMs() * (long) Math.pow(2, retryCount);
                        try {
                            logger.warn("Failed to send email (attempt {}/{}), retrying in {} ms: {}", 
                                retryCount + 1, getMaxRetries(), delay, e.getMessage());
                            Thread.sleep(delay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Email sending was interrupted", ie);
                        }
                    }
                    retryCount++;
                }
            }
            
            if (!sent && lastError != null) {
                meterRegistry.counter("email.error", "type", "send_failed").increment();
                logger.error("Failed to send email to {} after {} attempts: {}", 
                            to, getMaxRetries(), lastError.getMessage(), lastError);
                throw new RuntimeException("Failed to send email after " + getMaxRetries() + " attempts", lastError);
            }
            
        }, emailExecutor);
    }
    
    public CompletableFuture<Void> sendBulkEmail(List<String> toList, String subject, 
                                               String templateName, Map<String, Object> templateVars) {
        return sendBulkEmail(toList, subject, templateName, templateVars, DEFAULT_BATCH_SIZE);
    }
    
    /**
     * Sends emails in batches with rate limiting and retry logic.
     * 
     * @param toList List of recipient email addresses
     * @param subject Email subject
     * @param templateName Name of the email template
     * @param templateVars Template variables
     * @param batchSize Number of emails to process in each batch
     * @return CompletableFuture that completes when all emails are sent
     */
    @Async
    public CompletableFuture<Void> sendBulkEmail(List<String> toList, String subject, 
                                               String templateName, Map<String, Object> templateVars,
                                               int batchSize) {
        if (toList == null || toList.isEmpty()) {
            logger.warn("No recipient email addresses provided");
            return CompletableFuture.completedFuture(null);
        }

        // Filter out null or empty emails and make them unique
        List<String> uniqueEmails = toList.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (uniqueEmails.isEmpty()) {
            logger.warn("No valid email addresses found in the provided list");
            return CompletableFuture.completedFuture(null);
        }

        logger.info("Sending bulk email to {} recipients with subject: {}", uniqueEmails.size(), subject);
        meterRegistry.counter("email.bulk.send.attempt", "total", String.valueOf(uniqueEmails.size())).increment();

        // Process emails in batches
        List<CompletableFuture<Void>> batchFutures = new ArrayList<>();
        for (int i = 0; i < uniqueEmails.size(); i += batchSize) {
            final int start = i;
            int end = Math.min(i + batchSize, uniqueEmails.size());
            List<String> batch = uniqueEmails.subList(start, end);
            
            // Create a future for this batch
            CompletableFuture<Void> batchFuture = CompletableFuture.runAsync(() -> {
                logger.debug("Processing batch {}-{} of {}", start, end - 1, uniqueEmails.size());
                batch.forEach(email -> {
                    try {
                        // Apply rate limiting with timeout
                        if (!tryAcquireToken("")) {
                            logger.warn("Global rate limit exceeded for email sending");
                            Thread.sleep(1000); // Wait before retry
                            if (!tryAcquireToken("")) {
                                throw new RuntimeException("Global rate limit exceeded after retry");
                            }
                        }
                        
                        // Apply domain-specific rate limiting
                        String domain = email.substring(email.indexOf('@') + 1);
                        if (!tryAcquireToken(domain)) {
                            logger.warn("Rate limit exceeded for domain: {}", domain);
                            Thread.sleep(1000); // Wait before retry
                            if (!tryAcquireToken(domain)) {
                                throw new RuntimeException("Rate limit exceeded for domain after retry: " + domain);
                            }
                        }
                        
                        // Send email with retry
                        withRetry(() -> {
                            Timer.Sample sample = Timer.start();
                            try {
                                sendEmail(email, subject, templateName, new HashMap<>(templateVars));
                                meterRegistry.counter("email.send.success", "domain", domain).increment();
                                return null;
                            } finally {
                                sample.stop(emailSendTimer);
                            }
                        }, MAX_RETRIES, RETRY_DELAY_MS);
                    } catch (Exception e) {
                        logger.error("Failed to send email to {} after {} retries: {}", 
                                   email, MAX_RETRIES, e.getMessage());
                        meterRegistry.counter("email.send.failure", "domain", 
                            email.contains("@") ? email.substring(email.indexOf('@') + 1) : "unknown")
                            .increment();
                    }
                });
            }, emailExecutor);
            
            batchFutures.add(batchFuture);
            
            // Add a small delay between batches to avoid overwhelming the system
            if (i + batchSize < uniqueEmails.size()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Batch processing was interrupted");
                    break;
                }
            }
        }

        // Return a future that completes when all batches are done
        return CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture<?>[0]))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Error in batch email processing: {}", ex.getMessage(), ex);
                    } else {
                        logger.info("Completed sending {} emails to {} recipients", 
                                   uniqueEmails.size(), 
                                   uniqueEmails.stream().distinct().count());
                    }
                });
    }
    
    /**
     * Sends an email with retry logic.
     * @param email The recipient email
     * @param payment The payment details
     * @return The result of the email sending operation
     */
    private EmailSendingResult sendEmailWithRetry(String email, VendorPayment payment) {
        int attempt = 0;
        Exception lastError = null;
        
        while (attempt < MAX_RETRIES) {
            try {
                // Record metrics for each attempt
                meterRegistry.counter("email.send.attempt", "status", "started").increment();
                
                // Send the email and return the result
                EmailSendingResult result = sendEmailInternal(email, payment);
                if (result.isSuccess()) {
                    meterRegistry.counter("email.send.attempt", "status", "success").increment();
                    return result;
                }
                
                // If we got here, there was an error
                lastError = new RuntimeException(result.getMessage());
                
            } catch (Exception e) {
                lastError = e;
                logger.warn("Attempt {} failed to send email to {}: {}", 
                    attempt + 1, email, e.getMessage());
            }
            
            // Wait before retry
            if (attempt < MAX_RETRIES - 1) {
                try {
                    Thread.sleep(RETRY_DELAY_MS * (attempt + 1)); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return new EmailSendingResult(false, "Email sending was interrupted");
                }
            }
            
            attempt++;
            meterRegistry.counter("email.send.attempt", "status", "retry").increment();
        }
        
        // If we get here, all retries failed
        String errorMsg = String.format("Failed to send email to %s after %d attempts: %s", 
            email, MAX_RETRIES, lastError != null ? lastError.getMessage() : "Unknown error");
        
        meterRegistry.counter("email.send.attempt", "status", "failed").increment();
        logger.error(errorMsg, lastError);
        
        return new EmailSendingResult(false, errorMsg);
    }
    
    /**
     * Internal method to send a single email.
     */
    private EmailSendingResult sendEmailInternal(String email, VendorPayment payment) {
        return emailSendTimer.record(() -> {
            try {
                // Extract domain from email
                String domain = email.substring(email.indexOf('@') + 1);
                
                // Get the appropriate mail sender for the domain
                JavaMailSender mailSender = emailSenderFactory.getMailSender(domain);
                
                // Create and configure the email message
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);
                
                helper.setTo(email);
                helper.setFrom(emailConfig.getDefaultConfig().getFrom(), 
                              emailConfig.getDefaultConfig().getFromName());
                
                // Set email subject and content
                String subject = String.format("Payment Confirmation - %s", 
                    payment.getPaymentReference());
                helper.setSubject(subject);
                
                // Prepare template variables
                Map<String, Object> variables = new HashMap<>();
                variables.put("payment", payment);
                variables.put("vendor", payment.getVendor());
                
                // Process the email template
                Context context = new Context();
                context.setVariables(variables);
                String content = templateEngine.process("email/payment-confirmation", context);
                    
                helper.setText(content, true);
                
                // Send the email using the mail sender we already have
                mailSender.send(message);
                
                return new EmailSendingResult(true, "Email sent successfully");
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Helper method to execute an operation with retry logic.
     */
    private <T> T withRetry(Callable<T> operation, int maxRetries, long delayMs) throws Exception {
        int attempts = 0;
        Exception lastException;
        long currentDelay = delayMs;
        
        while (true) {
            try {
                return operation.call();
            } catch (Exception e) {
                lastException = e;
                attempts++;
                
                // Check if this is a rate limit error
                boolean isRateLimit = e.getMessage() != null && e.getMessage().contains("Rate limit");
                
                if (isRateLimit) {
                    logger.warn("Rate limit exceeded (attempt {}/{}): {}", attempts, maxRetries, e.getMessage());
                } else {
                    logger.warn("Operation failed (attempt {}/{}): {}", attempts, maxRetries, e.getMessage());
                }
                
                if (attempts > maxRetries) {
                    break;
                }
                
                try {
                    logger.warn("Retrying in {}ms...", currentDelay);
                    Thread.sleep(currentDelay);
                    // Exponential backoff with a maximum delay
                    currentDelay = Math.min(currentDelay * 2, MAX_RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    String errorMsg = isRateLimit ? 
                        "Operation was interrupted during rate limit wait" : 
                        "Operation was interrupted during retry wait";
                    throw new RuntimeException(errorMsg, ie);
                }
            }
        }
        
        throw lastException;
    }
    

    @FunctionalInterface
    private interface Callable<T> {
        T call() throws Exception;
    }
    
    /**
     * Attempts to acquire a token from the rate limiter with retry logic.
     * 
     * @param domain The email domain for rate limiting (not used in this implementation)
     * @return true if a token was acquired, false otherwise
     */
    private boolean tryAcquireToken(String domain) {
        // Use the rate limiter to check if we can send an email
        try {
            // Always use the rate limiter if it's available
            boolean allowed = rateLimiter.acquirePermission();
            if (allowed) {
                meterRegistry.counter("email.rate_limit.allowed").increment();
            } else {
                meterRegistry.counter("email.rate_limit.exceeded").increment();
            }
            return allowed;
        } catch (Exception e) {
            logger.warn("Rate limiter error: {}", e.getMessage());
            meterRegistry.counter("email.rate_limit.exceeded").increment();
            
            // Add some jitter to prevent thundering herd
            try {
                Thread.sleep((long) (Math.random() * 100)); // Add up to 100ms jitter
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
            
            return false;
        }
    }

    /**
     * Sends a simple email with the given subject and content.
     * 
     * @param to Recipient email address
     * @param subject Email subject
     * @param content Email content (HTML)
     */
    @Async
    public void sendSimpleEmail(String to, String subject, String content) {
        try {
            // Extract domain for rate limiting
            String domain = to.contains("@") ? to.substring(to.indexOf('@') + 1) : "default";
            
            // Get the appropriate mail sender for the domain
            JavaMailSender mailSender = emailSenderFactory.getMailSender(domain);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // Set email properties
            helper.setTo(to);
            helper.setSubject(subject);
            
            // Set from address from configuration
            String fromEmail = emailConfig.getDefaultConfig().getFrom();
            String fromName = emailConfig.getDefaultConfig().getFromName();
            if (fromName != null && !fromName.isEmpty()) {
                helper.setFrom(String.format("%s <%s>", fromName, fromEmail));
            } else {
                helper.setFrom(fromEmail);
            }
            
            // Set email content
            helper.setText(content, true);
            
            // Record metrics for the email send operation
            Timer.Sample timer = Timer.start(meterRegistry);
            try {
                mailSender.send(message);
                timer.stop(meterRegistry.timer("email.send.time", "status", "success"));
                meterRegistry.counter("email.sent", "status", "success").increment();
                logger.info("Email sent successfully to {}", to);
            } catch (Exception e) {
                timer.stop(meterRegistry.timer("email.send.time", "status", "error"));
                meterRegistry.counter("email.sent", "status", "error").increment();
                logger.error("Failed to send email to {}: {}", to, e.getMessage(), e);
                throw new RuntimeException("Failed to send email", e);
            }
        } catch (Exception e) {
            meterRegistry.counter("email.error", "type", "send_failed").increment();
            logger.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
