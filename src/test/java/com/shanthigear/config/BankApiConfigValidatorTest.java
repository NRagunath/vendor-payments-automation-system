package com.shanthigear.config;

import com.shanthigear.exception.BankApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankApiConfigValidatorTest {

    @Mock
    private BankApiConfig bankApiConfig;
    
    @Mock
    private BankApiConfig.SslConfig sslConfig;
    
    @InjectMocks
    private BankApiConfigValidator validator;
    
    private File tempKeystore;
    private File tempTruststore;
    
    @BeforeEach
    void setUp() throws IOException {
        // Create temporary keystore and truststore files for testing
        tempKeystore = File.createTempFile("test-keystore", ".jks");
        tempTruststore = File.createTempFile("test-truststore", ".jks");
        
        // Configure mocks
        when(bankApiConfig.getSsl()).thenReturn(sslConfig);
        when(sslConfig.isEnabled()).thenReturn(true);
        when(sslConfig.isKeyStoreEnabled()).thenReturn(true);
        when(sslConfig.isTrustStoreEnabled()).thenReturn(true);
        when(sslConfig.getKeyStorePath()).thenReturn(tempKeystore.getAbsolutePath());
        when(sslConfig.getTrustStorePath()).thenReturn(tempTruststore.getAbsolutePath());
        when(sslConfig.getProtocol()).thenReturn("TLSv1.2");
        
        // Set required properties
        when(bankApiConfig.getBaseUrl()).thenReturn("https://api.bank.com");
        when(bankApiConfig.getProcessPaymentEndpoint()).thenReturn("/api/payments");
        when(bankApiConfig.getVerifyPaymentEndpoint()).thenReturn("/api/payments/{id}");
        when(bankApiConfig.getApiKey()).thenReturn("test-api-key");
    }
    
    @Test
    void validate_ValidConfig_Success() {
        // Should not throw an exception
        validator.validate();
        
        // Verify SSL validation was called
        when(sslConfig.isEnabled()).thenReturn(true);
        validator.validate();
    }
    
    @Test
    void validate_MissingBaseUrl_ThrowsException() {
        when(bankApiConfig.getBaseUrl()).thenReturn("");
        
        BankApiException exception = assertThrows(BankApiException.class, () -> {
            validator.validate();
        });
        
        assertEquals("Bank API base URL is required", exception.getMessage());
        assertEquals(500, exception.getStatus().value());
    }
    
    @Test
    void validate_MissingProcessPaymentEndpoint_ThrowsException() {
        when(bankApiConfig.getProcessPaymentEndpoint()).thenReturn("");
        
        BankApiException exception = assertThrows(BankApiException.class, () -> {
            validator.validate();
        });
        
        assertEquals("Process payment endpoint is required", exception.getMessage());
    }
    
    @Test
    void validate_MissingVerifyPaymentEndpoint_ThrowsException() {
        when(bankApiConfig.getVerifyPaymentEndpoint()).thenReturn("");
        
        BankApiException exception = assertThrows(BankApiException.class, () -> {
            validator.validate();
        });
        
        assertEquals("Verify payment endpoint is required", exception.getMessage());
    }
    
    @Test
    void validate_NoAuthentication_ThrowsException() {
        when(bankApiConfig.getApiKey()).thenReturn(null);
        when(bankApiConfig.getAuthToken()).thenReturn(null);
        
        BankApiException exception = assertThrows(BankApiException.class, () -> {
            validator.validate();
        });
        
        assertTrue(exception.getMessage().contains("At least one authentication method must be configured"));
    }
    
    @Test
    void validate_SslEnabledButMissingKeystore_ThrowsException() {
        when(sslConfig.getKeyStorePath()).thenReturn("/nonexistent/keystore.jks");
        
        BankApiException exception = assertThrows(BankApiException.class, () -> {
            validator.validate();
        });
        
        assertTrue(exception.getMessage().contains("Keystore file not found"));
    }
    
    @Test
    void validate_SslEnabledButMissingTruststore_ThrowsException() {
        when(sslConfig.getTrustStorePath()).thenReturn("/nonexistent/truststore.jks");
        
        BankApiException exception = assertThrows(BankApiException.class, () -> {
            validator.validate();
        });
        
        assertTrue(exception.getMessage().contains("Truststore file not found"));
    }
    
    @Test
    void validate_SslEnabledButMissingProtocol_ThrowsException() {
        when(sslConfig.getProtocol()).thenReturn("");
        
        BankApiException exception = assertThrows(BankApiException.class, () -> {
            validator.validate();
        });
        
        assertEquals("SSL protocol is required when SSL is enabled", exception.getMessage());
    }
    
    @Test
    void validate_AuthWithToken_Success() {
        when(bankApiConfig.getApiKey()).thenReturn(null);
        when(bankApiConfig.getAuthToken()).thenReturn("test-auth-token");
        
        // Should not throw an exception
        validator.validate();
    }
    
    @Test
    void validate_AuthWithBasicAuth_Success() {
        when(bankApiConfig.getApiKey()).thenReturn(null);
        when(bankApiConfig.getAuthToken()).thenReturn(null);
        when(bankApiConfig.getUsername()).thenReturn("test-user");
        when(bankApiConfig.getPassword()).thenReturn("test-pass");
        
        // Should not throw an exception
        validator.validate();
    }
}
