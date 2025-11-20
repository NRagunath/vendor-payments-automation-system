package com.shanthigear.util;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

class SecureLoggingUtilsTest {
    
    private static final Logger log = LoggerFactory.getLogger(SecureLoggingUtilsTest.class);
    
    @Test
    void maskSensitiveData_PAN() {
        String input = "Processing payment with card 4111111111111111";
        String expected = "Processing payment with card 4111********1111";
        assertEquals(expected, SecureLoggingUtils.maskSensitiveData(input));
    }
    
    @Test
    void maskSensitiveData_CVV() {
        String input = "CVV: 123";
        String expected = "CVV: [MASKED]";
        assertEquals(expected, SecureLoggingUtils.maskSensitiveData(input));
    }
    
    @Test
    void maskSensitiveData_ApiKey() {
        String input = "API Key: sk_test_1234567890abcdefghijklmnopqrstuvwxyz";
        String expected = "API Key: [MASKED]";
        assertEquals(expected, SecureLoggingUtils.maskSensitiveData(input));
    }
    
    @Test
    void maskSensitiveData_Password() {
        String input = "password=mySecretPassword123";
        String expected = "password= [MASKED]";
        assertTrue(SecureLoggingUtils.maskSensitiveData(input).startsWith(expected));
    }
    
    @Test
    void maskString_Short() {
        assertEquals("***", SecureLoggingUtils.maskString("123"));
    }
    
    @Test
    void maskString_Long() {
        assertEquals("1234*****6789", SecureLoggingUtils.maskString("123456789"));
    }
    
    @Test
    void maskString_Empty() {
        assertEquals("", SecureLoggingUtils.maskString(""));
    }
    
    @Test
    void maskString_Null() {
        assertNull(SecureLoggingUtils.maskString(null));
    }
    
    @Test
    void secureLoggingMethods() {
        // These just verify the methods don't throw exceptions
        SecureLoggingUtils.info(log, "Test info with PAN: %s", "4111111111111111");
        SecureLoggingUtils.debug(log, "Test debug with CVV: %s", "123");
        SecureLoggingUtils.error(log, "Test error with API key: %s", "sk_test_1234567890");
        SecureLoggingUtils.error(log, "Test error with exception", new RuntimeException("Test exception"));
    }
}
