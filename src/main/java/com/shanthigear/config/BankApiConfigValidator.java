package com.shanthigear.config;

import com.shanthigear.exception.BankApiException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Validates the bank API configuration at application startup.
 * Ensures all required properties are properly configured.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BankApiConfigValidator {

    private final BankApiConfig bankApiConfig;
    
    /**
     * Validates the bank API configuration after the bean is initialized.
     * @throws BankApiException if the configuration is invalid
     */
    @PostConstruct
    public void validate() {
        log.info("Validating bank API configuration...");
        
        // Validate base URL
        if (StringUtils.isBlank(bankApiConfig.getBaseUrl())) {
            throw new BankApiException("Bank API base URL is required", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        // Validate endpoints
        if (StringUtils.isBlank(bankApiConfig.getProcessPaymentEndpoint())) {
            throw new BankApiException("Process payment endpoint is required", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        if (StringUtils.isBlank(bankApiConfig.getVerifyPaymentEndpoint())) {
            throw new BankApiException("Verify payment endpoint is required", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        // Validate authentication method
        validateAuthentication();
        
        // Validate SSL configuration if enabled
        if (bankApiConfig.getSsl().isEnabled()) {
            validateSslConfig();
        }
        
        log.info("Bank API configuration validated successfully");
    }
    
    /**
     * Validates that at least one authentication method is configured.
     */
    private void validateAuthentication() {
        boolean hasApiKey = StringUtils.isNotBlank(bankApiConfig.getApiKey());
        boolean hasAuthToken = StringUtils.isNotBlank(bankApiConfig.getAuthToken());
        boolean hasBasicAuth = StringUtils.isNotBlank(bankApiConfig.getUsername()) && 
                              StringUtils.isNotBlank(bankApiConfig.getPassword());
        
        if (!hasApiKey && !hasAuthToken && !hasBasicAuth) {
            throw new BankApiException(
                "At least one authentication method must be configured (API key, auth token, or basic auth)",
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * Validates the SSL configuration.
     */
    private void validateSslConfig() {
        BankApiConfig.SslConfig ssl = bankApiConfig.getSsl();
        
        // Validate keystore
        if (ssl.isKeyStoreEnabled()) {
            if (StringUtils.isBlank(ssl.getKeyStorePath())) {
                throw new BankApiException("Keystore path is required when SSL is enabled", 
                    HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
            File keyStoreFile = new File(ssl.getKeyStorePath());
            if (!keyStoreFile.exists() || !keyStoreFile.isFile()) {
                throw new BankApiException("Keystore file not found: " + ssl.getKeyStorePath(), 
                    HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
            if (StringUtils.isBlank(ssl.getKeyStorePassword())) {
                log.warn("Keystore password is not set. This may cause SSL handshake failures.");
            }
        }
        
        // Validate truststore
        if (ssl.isTrustStoreEnabled()) {
            if (StringUtils.isBlank(ssl.getTrustStorePath())) {
                throw new BankApiException("Truststore path is required when SSL is enabled", 
                    HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
            File trustStoreFile = new File(ssl.getTrustStorePath());
            if (!trustStoreFile.exists() || !trustStoreFile.isFile()) {
                throw new BankApiException("Truststore file not found: " + ssl.getTrustStorePath(), 
                    HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
            if (StringUtils.isBlank(ssl.getTrustStorePassword())) {
                log.warn("Truststore password is not set. This may cause SSL handshake failures.");
            }
        }
        
        // Validate protocol
        if (StringUtils.isBlank(ssl.getProtocol())) {
            throw new BankApiException("SSL protocol is required when SSL is enabled", 
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
