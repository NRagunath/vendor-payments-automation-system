package com.shanthigear.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for the bank API.
 */
@Configuration
@ConfigurationProperties(prefix = "bank.api")
public class BankApiConfig {
    
    // Base API configuration
    private String baseUrl;
    private String apiKey;
    private String authToken;
    private String username;
    private String password;
    private String clientId;
    private String processPaymentEndpoint;
    private String verifyPaymentEndpoint;
    private int connectTimeout = 5000; // 5 seconds
    private int readTimeout = 30000;    // 30 seconds
    
    // SSL Configuration
    private SslConfig ssl = new SslConfig();
    
    // Retry configuration
    private RetryConfig retry = new RetryConfig();
    
    // HTTP headers
    private Map<String, String> headers = new HashMap<>();
    
    // Nested configuration classes
    public static class SslConfig {
        private boolean enabled = true;
        private String trustStore;
        private String trustStorePassword;
        private String trustStoreType = "JKS";
        private String keyStore;
        private String keyStorePassword;
        private String keyStoreType = "JKS";
        private String keyPassword;
        private String keyAlias;
        private String protocol = "TLSv1.2";
        private boolean trustStoreEnabled = true;
        private boolean keyStoreEnabled = true;
        private String trustStorePath;
        private String keyStorePath;
        
        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getTrustStore() { return trustStore; }
        public void setTrustStore(String trustStore) { this.trustStore = trustStore; }
        public String getTrustStorePassword() { return trustStorePassword; }
        public void setTrustStorePassword(String trustStorePassword) { this.trustStorePassword = trustStorePassword; }
        public String getKeyStore() { return keyStore; }
        public boolean isTrustStoreEnabled() { return trustStoreEnabled; }
        public void setTrustStoreEnabled(boolean trustStoreEnabled) { this.trustStoreEnabled = trustStoreEnabled; }
        public boolean isKeyStoreEnabled() { return keyStoreEnabled; }
        public void setKeyStoreEnabled(boolean keyStoreEnabled) { this.keyStoreEnabled = keyStoreEnabled; }
        public String getTrustStorePath() { 
            return trustStorePath != null ? trustStorePath : trustStore; 
        }
        public void setTrustStorePath(String trustStorePath) { this.trustStorePath = trustStorePath; }
        public String getKeyStorePath() { 
            return keyStorePath != null ? keyStorePath : keyStore; 
        }
        public void setKeyStorePath(String keyStorePath) { this.keyStorePath = keyStorePath; }
        public void setKeyStore(String keyStore) { this.keyStore = keyStore; }
        public String getKeyStorePassword() { return keyStorePassword; }
        public void setKeyStorePassword(String keyStorePassword) { this.keyStorePassword = keyStorePassword; }
        public String getKeyPassword() { return keyPassword; }
        public void setKeyPassword(String keyPassword) { this.keyPassword = keyPassword; }
        public String getKeyAlias() { return keyAlias; }
        public void setKeyAlias(String keyAlias) { this.keyAlias = keyAlias; }
        public String getProtocol() { return protocol; }
        public void setProtocol(String protocol) { this.protocol = protocol; }
        public String getKeyStoreType() { return keyStoreType; }
        public void setKeyStoreType(String keyStoreType) { this.keyStoreType = keyStoreType; }
        public String getTrustStoreType() { return trustStoreType; }
        public void setTrustStoreType(String trustStoreType) { this.trustStoreType = trustStoreType; }
    }
    
    public static class RetryConfig {
        private int maxAttempts = 3;
        private long initialInterval = 1000; // 1 second
        private double multiplier = 2.0;
        private long maxInterval = 5000; // 5 seconds
        
        // Getters and Setters
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public long getInitialInterval() { return initialInterval; }
        public void setInitialInterval(long initialInterval) { this.initialInterval = initialInterval; }
        public double getMultiplier() { return multiplier; }
        public void setMultiplier(double multiplier) { this.multiplier = multiplier; }
        public long getMaxInterval() { return maxInterval; }
        public void setMaxInterval(long maxInterval) { this.maxInterval = maxInterval; }
    }

    // Getters and Setters for top-level properties
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getProcessPaymentEndpoint() {
        return processPaymentEndpoint;
    }

    public void setProcessPaymentEndpoint(String processPaymentEndpoint) {
        this.processPaymentEndpoint = processPaymentEndpoint;
    }

    public String getVerifyPaymentEndpoint() {
        return verifyPaymentEndpoint;
    }

    public void setVerifyPaymentEndpoint(String verifyPaymentEndpoint) {
        this.verifyPaymentEndpoint = verifyPaymentEndpoint;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
    
    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public SslConfig getSsl() {
        return ssl;
    }
    
    public void setSsl(SslConfig ssl) {
        this.ssl = ssl;
    }
    
    public RetryConfig getRetry() {
        return retry;
    }
    
    public void setRetry(RetryConfig retry) {
        this.retry = retry;
    }
    
    /**
     * Gets the additional headers to be included in API requests.
     * @return Map of header names to values
     */
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    /**
     * Sets the additional headers to be included in API requests.
     * @param headers Map of header names to values
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
    }
    
    /**
     * Adds a header to be included in API requests.
     * @param name The header name
     * @param value The header value
     */
    public void addHeader(String name, String value) {
        if (name != null && value != null) {
            this.headers.put(name, value);
        }
    }
    
    /**
     * Removes a header from the API requests.
     * @param name The header name to remove
     */
    public void removeHeader(String name) {
        if (name != null) {
            this.headers.remove(name);
        }
    }
    
    // Convenience methods for retry configuration
    public int getRetryMaxAttempts() {
        return retry.getMaxAttempts();
    }
    
    public long getRetryInitialInterval() {
        return retry.getInitialInterval();
    }
    
    public double getRetryMultiplier() {
        return retry.getMultiplier();
    }
    
    public long getRetryMaxInterval() {
        return retry.getMaxInterval();
    }
    
    /**
     * Validates that the configuration is complete and usable.
     * @throws IllegalStateException if required configuration is missing
     */
    public void validate() {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalStateException("Bank API base URL is required");
        }
        
        // Validate endpoints
        if (processPaymentEndpoint == null || processPaymentEndpoint.trim().isEmpty()) {
            throw new IllegalStateException("Process payment endpoint is required");
        }
        
        if (verifyPaymentEndpoint == null || verifyPaymentEndpoint.trim().isEmpty()) {
            throw new IllegalStateException("Verify payment endpoint is required");
        }
        
        // Validate timeouts
        if (connectTimeout <= 0) {
            throw new IllegalStateException("Connect timeout must be positive");
        }
        
        if (readTimeout <= 0) {
            throw new IllegalStateException("Read timeout must be positive");
        }
        
        // Validate SSL configuration if enabled
        if (ssl.isEnabled()) {
            if (ssl.getKeyStore() == null || ssl.getKeyStore().trim().isEmpty() || 
                ssl.getKeyStorePassword() == null || ssl.getKeyStorePassword().trim().isEmpty()) {
                throw new IllegalStateException(
                    "SSL is enabled but key store configuration is incomplete. " +
                    "Please configure key-store and key-store-password");
            }
        }
        
        // Validate authentication
        boolean hasApiKey = StringUtils.hasText(apiKey);
        boolean hasBasicAuth = StringUtils.hasText(username) && StringUtils.hasText(password);
        boolean hasBearerToken = StringUtils.hasText(authToken);
        
        if (!hasApiKey && !hasBasicAuth && !hasBearerToken) {
            throw new IllegalStateException(
                "No authentication method configured for bank API. " +
                "Please configure at least one of: api-key, basic auth (username/password), or auth-token");
        }
    }
}
