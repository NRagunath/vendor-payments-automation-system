package com.shanthigear.health;

import com.shanthigear.service.EmailSenderFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Health indicator for the email service.
 */
@Component
public class EmailServiceHealthIndicator implements HealthIndicator {

    private final EmailSenderFactory emailSenderFactory;

    public EmailServiceHealthIndicator(EmailSenderFactory emailSenderFactory) {
        this.emailSenderFactory = emailSenderFactory;
    }

    @Override
    public Health health() {
        try {
            // Get cache stats (this is a simplified example)
            // In a real implementation, you might want to check actual connectivity to SMTP servers
            Map<String, Object> details = Map.of(
                "status", "UP",
                "cachedSenders", emailSenderFactory.getCacheSize(),
                "message", "Email service is running"
            );
            
            return Health.up()
                .withDetails(details)
                .build();
                
        } catch (Exception e) {
            return Health.down(e)
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
