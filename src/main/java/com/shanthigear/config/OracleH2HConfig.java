package com.shanthigear.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Oracle Host-to-Host (H2H) integration.
 * These properties should be configured in application.yml or application.properties.
 */
@Configuration
@ConfigurationProperties(prefix = "oracle.h2h")
@Data
public class OracleH2HConfig {
    
    /**
     * Whether the Oracle H2H integration is enabled
     */
    private boolean enabled = false;
    
    /**
     * Oracle H2H API base URL
     */
    private String baseUrl;
    
    /**
     * Username for Oracle H2H authentication
     */
    private String username;
    
    /**
     * Password for Oracle H2H authentication
     */
    private String password;
    
    /**
     * Authentication token for API access (if using token-based auth)
     */
    private String authToken;
    
    /**
     * Client ID for API authentication
     */
    private String clientId;
    
    /**
     * Connection timeout in milliseconds
     */
    private int connectionTimeout = 30000;
    
    /**
     * Read timeout in milliseconds
     */
    private int readTimeout = 60000;
    
    /**
     * Maximum number of retry attempts for failed requests
     */
    private int maxRetryAttempts = 3;
    
    /**
     * Retry delay in milliseconds between attempts
     */
    private long retryDelay = 5000;
    
    /**
     * Batch size for processing payments in bulk
     */
    private int batchSize = 50;
    
    /**
     * Path to the keystore file for SSL/TLS
     */
    private String keystorePath;
    
    /**
     * Keystore password
     */
    private String keystorePassword;
    
    /**
     * Truststore path
     */
    private String truststorePath;
    
    /**
     * Truststore password
     */
    private String truststorePassword;
    
    /**
     * Whether to enable SSL certificate validation
     */
    private boolean sslValidationEnabled = true;
    
    /**
     * Payment template ID in Oracle
     */
    private String paymentTemplateId;
    
    /**
     * Default currency code (e.g., "INR")
     */
    private String defaultCurrency = "INR";
    
    /**
     * Default payment method (e.g., "BANK_TRANSFER")
     */
    private String defaultPaymentMethod = "BANK_TRANSFER";
    
    /**
     * Whether to enable debug logging for requests/responses
     */
    private boolean debugLogging = false;
    
    /**
     * Endpoint paths for different operations
     */
    private final Endpoints endpoints = new Endpoints();
    
    public String getAuthToken() {
        return authToken;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    @Data
    public static class Endpoints {
        /**
         * Endpoint for processing single payments
         */
        private String processPayment = "/api/v1/payments/process";
        
        /**
         * Endpoint for batch payment processing
         */
        private String processBatch = "/api/v1/payments/batch";
        
        /**
         * Endpoint for checking payment status
         */
        private String checkStatus = "/api/v1/payments/status/{referenceNumber}";
        
        /**
         * Endpoint for retrieving payment details
         */
        private String getPayment = "/api/v1/payments/{referenceNumber}";
        
        /**
         * Endpoint for checking service health/availability
         */
        private String healthCheck = "/api/v1/health";
    }
}
