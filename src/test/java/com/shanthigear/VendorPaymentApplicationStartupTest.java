package com.shanthigear;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import com.shanthigear.config.AppProperties;
import com.shanthigear.repository.VendorRepository;
import com.shanthigear.service.PaymentProcessingService;
import com.shanthigear.service.VendorImportService;

@SpringBootTest
@ActiveProfiles("test")
class VendorPaymentApplicationStartupTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private VendorRepository vendorRepository;

    @MockBean
    private PaymentProcessingService paymentProcessingService;

    @MockBean
    private VendorImportService vendorImportService;

    @Autowired
    private AppProperties appProperties;

    @Test
    void contextLoads() {
        // This test will fail if the application context cannot start
        assertNotNull(applicationContext, "Application context should be loaded");
    }

    @Test
    void verifyBeanInitialization() {
        // Verify that required beans are initialized
        assertNotNull(applicationContext.getBean(AppProperties.class), "AppProperties should be initialized");
        assertNotNull(applicationContext.getBean(VendorRepository.class), "VendorRepository should be initialized");
        assertNotNull(applicationContext.getBean(PaymentProcessingService.class), "PaymentProcessingService should be initialized");
        assertNotNull(applicationContext.getBean(VendorImportService.class), "VendorImportService should be initialized");
    }

    @Test
    void verifyConfigurationPropertiesBinding() {
        // Verify that configuration properties are bound correctly
        assertNotNull(appProperties.getPayment(), "Payment properties should be bound");
        assertNotNull(appProperties.getSecurity(), "Security properties should be bound");
        assertNotNull(appProperties.getNotification(), "Notification properties should be bound");
        
        // Verify some default values
        assertEquals("INR", appProperties.getPayment().getDefaultCurrency(), "Default currency should be INR");
        // Check if scheduling is enabled by default
        assertTrue(appProperties.getScheduling().isEnabled(), "Scheduling should be enabled by default");
    }

    @Test
    void verifyScheduledTasksAreRegistered() {
        // This test verifies that scheduled tasks are properly registered
        // The actual scheduling is tested in the ScheduledTasksTest
        assertDoesNotThrow(() -> {
            applicationContext.getBean("scheduledTasks");
        }, "Scheduled tasks should be registered");
    }

    @Test
    void verifyWebMvcConfiguration() {
        // Verify that WebMvc configuration is applied
        assertDoesNotThrow(() -> {
            applicationContext.getBean("webMvcConfigurer");
        }, "WebMvcConfigurer should be registered");
    }

    @Test
    void verifySecurityConfiguration() {
        // Verify that security configuration is applied
        assertDoesNotThrow(() -> {
            applicationContext.getBean("securityFilterChain");
        }, "Security filter chain should be configured");
    }

    @Test
    void verifyDatabaseConfiguration() {
        // Verify that database configuration is applied
        assertDoesNotThrow(() -> {
            applicationContext.getBean("dataSource");
            applicationContext.getBean("entityManagerFactory");
            applicationContext.getBean("transactionManager");
        }, "Database beans should be configured");
    }

    @Test
    void verifyOpenApiConfiguration() {
        // Verify that OpenAPI/Swagger configuration is applied
        assertDoesNotThrow(() -> {
            applicationContext.getBean("openAPI");
        }, "OpenAPI bean should be configured");
    }

    @Test
    void verifyAsyncConfiguration() {
        // Verify that async configuration is applied
        assertDoesNotThrow(() -> {
            applicationContext.getBean("taskExecutor");
            applicationContext.getBean("asyncUncaughtExceptionHandler");
        }, "Async configuration should be applied");
    }

    @Test
    void verifyValidationConfiguration() {
        // Verify that validation is configured
        assertDoesNotThrow(() -> {
            applicationContext.getBean("methodValidationPostProcessor");
        }, "Method validation should be configured");
    }
}
