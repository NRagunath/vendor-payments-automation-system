package com.shanthigear.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for payment processing.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {
    
    /**
     * Prefix for generating payment reference numbers.
     */
    private String referencePrefix = "PAY";
    
    /**
     * Maximum number of retry attempts for failed payments.
     */
    private int maxRetryAttempts = 3;
    
    /**
     * Delay between retry attempts in milliseconds.
     */
    private long retryDelay = 5000;
    
    /**
     * Whether to enable automatic retry for failed payments.
     */
    private boolean retryEnabled = true;
    
    /**
     * Default currency code for payments.
     */
    private String defaultCurrency = "USD";
    
    /**
     * Timeout for payment processing in milliseconds.
     */
    private long processingTimeout = 30000;
    
    /**
     * Whether to validate bank account details before processing payment.
     */
    private boolean validateBankAccount = true;
    
    /**
     * Whether to send email notifications for successful payments.
     */
    private boolean sendEmailNotifications = true;
}
