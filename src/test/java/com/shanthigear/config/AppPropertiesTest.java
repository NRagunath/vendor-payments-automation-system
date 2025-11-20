package com.shanthigear.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@ActiveProfiles("test")
@EnableConfigurationProperties(value = AppProperties.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@TestPropertySource(properties = {
    "app.payment.allowed-currencies=INR,USD,EUR",
    "app.payment.default-currency=INR",
    "app.payment.min-amount=10",
    "app.payment.max-amount=1000000",
    "app.payment.rate-limit=100",
    "app.payment.rate-limit-duration=PT1H",
    "app.notification.email.enabled=true",
    "app.notification.email.from=noreply@test.com",
    "app.notification.sms.enabled=false",
    "app.scheduling.enabled=true",
    "app.scheduling.pool-size=5",
    "app.scheduling.reconcile-payments-cron=0 0/5 * * * *",
    "app.scheduling.update-vendor-status-cron=0 0 0 * * *",
    "app.security.jwt.secret=test-secret-key",
    "app.security.jwt.expiration=86400000",
    "app.security.cors.allowed-origins=http://localhost:3000,http://localhost:8080",
    "app.security.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS"
})
class AppPropertiesTest {

    @Autowired
    private AppProperties appProperties;

    @Test
    void paymentProperties_ShouldBeBoundCorrectly() {
        AppProperties.PaymentProperties payment = appProperties.getPayment();
        assertNotNull(payment);
        
        assertEquals(List.of("INR", "USD", "EUR"), payment.getAllowedCurrencies());
        assertEquals("INR", payment.getDefaultCurrency());
        assertEquals(10, payment.getMinAmount());
        assertEquals(1_000_000, payment.getMaxAmount());
        assertEquals(100, payment.getRateLimit());
        assertEquals(Duration.ofHours(1), payment.getRateLimitDuration());
    }

    @Test
    void notificationProperties_ShouldBeBoundCorrectly() {
        AppProperties.NotificationProperties notification = appProperties.getNotification();
        assertNotNull(notification);
        
        assertTrue(notification.getEmail().isEnabled());
        assertEquals("noreply@test.com", notification.getEmail().getFrom());
        assertFalse(notification.getSms().isEnabled());
    }

    @Test
    void schedulingProperties_ShouldBeBoundCorrectly() {
        AppProperties.SchedulingProperties scheduling = appProperties.getScheduling();
        assertNotNull(scheduling);
        
        assertTrue(scheduling.isEnabled());
        assertEquals(5, scheduling.getPoolSize());
        assertEquals("0 0/5 * * * *", scheduling.getReconcilePaymentsCron());
        assertEquals("0 0 0 * * *", scheduling.getUpdateVendorStatusCron());
    }

    @Test
    void securityProperties_ShouldBeBoundCorrectly() {
        AppProperties.SecurityProperties security = appProperties.getSecurity();
        assertNotNull(security);
        
        assertEquals("test-secret-key", security.getJwt().getSecret());
        assertEquals(86400000, security.getJwt().getExpiration());
        
        assertArrayEquals(
            new String[]{"http://localhost:3000", "http://localhost:8080"},
            security.getCors().getAllowedOrigins()
        );
        
        assertArrayEquals(
            new String[]{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
            security.getCors().getAllowedMethods()
        );
    }

    @Test
    void nestedProperties_ShouldBeValidated() {
        AppProperties.PaymentProperties payment = appProperties.getPayment();
        assertThrows(IllegalStateException.class, () -> {
            payment.validate();
        });
    }

    @Test
    void toString_ShouldReturnNonEmptyString() {
        assertNotNull(appProperties.toString());
        assertFalse(appProperties.toString().isEmpty());
    }
}
