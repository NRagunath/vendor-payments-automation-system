package com.shanthigear.integration;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import com.shanthigear.service.PaymentReconciliationService;
import com.shanthigear.service.VendorStatusUpdateService;

@SpringBootTest
@ActiveProfiles("test")
class ScheduledTasksTest {

    @SpyBean
    private PaymentReconciliationService paymentReconciliationService;

    @SpyBean
    private VendorStatusUpdateService vendorStatusUpdateService;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(paymentReconciliationService, vendorStatusUpdateService);
        
        // Configure Awaitility
        Awaitility.setDefaultPollInterval(Duration.ofSeconds(1));
        Awaitility.setDefaultTimeout(Duration.ofSeconds(30));
    }

    @Test
    void reconcilePayments_ShouldRunOnSchedule() {
        // Wait for the scheduled task to run at least once
        await().atMost(2, TimeUnit.MINUTES)
                .untilAsserted(() -> verify(paymentReconciliationService, atLeastOnce())
                        .reconcilePendingPayments());
    }

    @Test
    void updateVendorStatuses_ShouldRunOnSchedule() {
        // Wait for the scheduled task to run at least once
        await().atMost(2, TimeUnit.MINUTES)
                .untilAsserted(() -> verify(vendorStatusUpdateService, atLeastOnce())
                        .updateInactiveVendors());
    }

    @Test
    void scheduledTasks_ShouldHandleExceptions() {
        // Make the method throw an exception
        doThrow(new RuntimeException("Test exception"))
                .when(paymentReconciliationService).reconcilePendingPayments();
        
        // The task should still be scheduled even if it fails
        await().atMost(2, TimeUnit.MINUTES)
                .untilAsserted(() -> verify(paymentReconciliationService, atLeast(2))
                        .reconcilePendingPayments());
    }

    @Test
    void scheduledTasks_ShouldNotRunInParallel() throws InterruptedException {
        // Make the method take some time to execute
        doAnswer(invocation -> {
            Thread.sleep(2000);
            return null;
        }).when(paymentReconciliationService).reconcilePendingPayments();
        
        // Wait for the first execution to start
        await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(paymentReconciliationService, atLeastOnce())
                        .reconcilePendingPayments());
        
        // Verify that the method is not executed in parallel
        verify(paymentReconciliationService, atMostOnce()).reconcilePendingPayments();
        
        // Wait for the next scheduled execution
        Thread.sleep(60000);
        
        // Now we should have at least 2 invocations (one that just finished and one new one)
        verify(paymentReconciliationService, atLeast(2)).reconcilePendingPayments();
    }
}
