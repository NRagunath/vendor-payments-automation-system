package com.shanthigear;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for Vendor Payment Notifier.
 * Enables Spring Boot, retry, and async processing.
 */
@SpringBootApplication
@EnableRetry
@EnableAsync
public class VendorPaymentApplication {
    
    /**
     * Main method to start the application.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(VendorPaymentApplication.class, args);
    }
}
