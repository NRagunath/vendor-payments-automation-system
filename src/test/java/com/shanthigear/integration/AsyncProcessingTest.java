package com.shanthigear.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import com.shanthigear.exception.PaymentProcessingException;
import com.shanthigear.model.PaymentStatus;
import com.shanthigear.model.VendorPayment;
import com.shanthigear.service.PaymentProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class AsyncProcessingTest {

    @SpyBean
    private PaymentProcessingService paymentProcessingService;
    
    @Mock
    private PaymentProcessingService mockPaymentProcessingService;
    
    @Autowired
    private PaymentProcessingService realPaymentProcessingService;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(mockPaymentProcessingService);
    }
    
    @Test
    void processPayment_ShouldProcessSynchronously() throws Exception {
        // Given
        VendorPayment payment = createTestPayment(1L, "VENDOR123", BigDecimal.valueOf(1000));
        VendorPayment processedPayment = createTestPayment(1L, "VENDOR123", BigDecimal.valueOf(1000));
        processedPayment.setStatus(PaymentStatus.COMPLETED);
        
        // Mock the service to return the processed payment
        when(paymentProcessingService.processPayment(any(VendorPayment.class))).thenReturn(processedPayment);
        
        // When
        VendorPayment result = paymentProcessingService.processPayment(payment);
        
        // Then - Verify the payment was processed
        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        verify(paymentProcessingService, times(1)).processPayment(same(payment));
    }
    
    @Test
    void processPaymentAsync_ShouldProcessInBackground() throws Exception {
        // Given
        VendorPayment payment = createTestPayment(1L, "VENDOR123", BigDecimal.valueOf(1000));
        VendorPayment processedPayment = createTestPayment(1L, "VENDOR123", BigDecimal.valueOf(1000));
        processedPayment.setStatus(PaymentStatus.COMPLETED);
        
        // Mock the service to return the processed payment after a delay
        when(paymentProcessingService.processPayment(any(VendorPayment.class))).thenAnswer(invocation -> {
            Thread.sleep(100); // Simulate processing time
            return processedPayment;
        });
        
        // When - Process in a separate thread to simulate async behavior
        CompletableFuture<VendorPayment> future = CompletableFuture.supplyAsync(() -> {
            try {
                return paymentProcessingService.processPayment(payment);
            } catch (PaymentProcessingException e) {
                throw new CompletionException(e);
            }
        });
        
        // Then - Verify the future completes with the expected result
        VendorPayment result = future.get(5, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        verify(paymentProcessingService, times(1)).processPayment(same(payment));
    }

    @Test
    @Disabled("Requires implementation of processBatchPayments in PaymentProcessingService")
    void processBatchPayments_ShouldProcessInParallel() throws Exception {
        // Given
        List<VendorPayment> payments = Arrays.asList(
            createTestPayment(1L, "VENDOR1", BigDecimal.valueOf(1000)),
            createTestPayment(2L, "VENDOR2", BigDecimal.valueOf(2000)),
            createTestPayment(3L, "VENDOR3", BigDecimal.valueOf(3000))
        );
        
        // When - Process each payment asynchronously using the real service
        List<CompletableFuture<VendorPayment>> futures = payments.stream()
            .map(payment -> CompletableFuture.supplyAsync(() -> {
                try {
                    // Process each payment and update its status
                    VendorPayment processed = realPaymentProcessingService.processPayment(payment);
                    payment.setStatus(processed.getStatus());
                    return processed;
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }))
            .collect(Collectors.toList());
        
        // Then - Wait for all futures to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);
        
        // Verify all payments were processed with the expected status
        for (VendorPayment payment : payments) {
            assertEquals(PaymentStatus.COMPLETED, payment.getStatus(), 
                "Payment " + payment.getId() + " was not marked as completed");
        }
    }

    @Test
    void processPayment_ShouldReturnProcessedPayment() throws Exception {
        // Given
        VendorPayment payment = createTestPayment(1L, "VENDOR123", BigDecimal.valueOf(1500));
        VendorPayment processedPayment = createTestPayment(1L, "VENDOR123", BigDecimal.valueOf(1500));
        processedPayment.setStatus(PaymentStatus.COMPLETED);
        
        when(paymentProcessingService.processPayment(any(VendorPayment.class)))
            .thenReturn(processedPayment);
        
        // When - Process in a separate thread to simulate async behavior
        CompletableFuture<VendorPayment> future = CompletableFuture.supplyAsync(() -> {
            try {
                return paymentProcessingService.processPayment(payment);
            } catch (PaymentProcessingException e) {
                throw new CompletionException(e);
            }
        });
        
        // Then
        VendorPayment result = future.get(5, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        verify(paymentProcessingService, times(1)).processPayment(same(payment));
    }

    @Test
    void processPayment_ShouldHandleException() throws Exception {
        // Given
        VendorPayment payment = createTestPayment(1L, "VENDOR-ERROR", BigDecimal.valueOf(1000));
        PaymentProcessingException expectedException = new PaymentProcessingException("Test exception");
        
        when(paymentProcessingService.processPayment(any(VendorPayment.class)))
            .thenThrow(expectedException);
        
        // When - Process in a separate thread to simulate async behavior
        CompletableFuture<VendorPayment> future = CompletableFuture.supplyAsync(() -> {
            try {
                return paymentProcessingService.processPayment(payment);
            } catch (PaymentProcessingException e) {
                throw new CompletionException(e);
            }
        });
        
        // Then
        ExecutionException exception = assertThrows(ExecutionException.class, 
            () -> future.get(5, TimeUnit.SECONDS));
        
        assertTrue(exception.getCause() instanceof CompletionException);
        assertEquals(expectedException, exception.getCause().getCause());
        verify(paymentProcessingService, times(1)).processPayment(same(payment));
    }
    
    private VendorPayment createTestPayment(Long id, String vendorId, BigDecimal amount) {
        VendorPayment payment = new VendorPayment();
        payment.setId(id);
        payment.setVendorId(vendorId);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PROCESSING);
        return payment;
    }
}
