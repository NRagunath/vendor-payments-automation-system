package com.shanthigear.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanthigear.config.BankApiConfig;
import com.shanthigear.dto.BankPaymentResponseDTO;
import com.shanthigear.exception.BankApiException;
import com.shanthigear.exception.PaymentProcessingException;
import com.shanthigear.payload.request.PaymentRequest;
import com.shanthigear.payload.response.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankPaymentServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private BankApiConfig bankApiConfig;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RetryTemplate retryTemplate;

    @InjectMocks
    private BankPaymentServiceImpl bankPaymentService;

    private PaymentRequest paymentRequest;
    private final BankPaymentResponseDTO successResponse = createSuccessResponse();
    private final BankPaymentResponseDTO pendingResponse = createPendingResponse();

    @BeforeEach
    void setUp() {
        // Setup test payment request
        paymentRequest = new PaymentRequest();
        paymentRequest.setAmount(new BigDecimal("1000.00"));
        paymentRequest.setCurrency("INR");
        paymentRequest.setDebitAccountNumber("1234567890");
        paymentRequest.setBeneficiaryAccountNumber("9876543210");
        paymentRequest.setBeneficiaryName("Test Beneficiary");
        paymentRequest.setBeneficiaryBankCode("HDFC0001234");
        paymentRequest.setDescription("Test payment");

        // Mock bank config
        when(bankApiConfig.getBaseUrl()).thenReturn("https://api.bank.com");
        when(bankApiConfig.getProcessPaymentEndpoint()).thenReturn("/v1/payments");
        when(bankApiConfig.getVerifyPaymentEndpoint()).thenReturn("/v1/payments/");
        when(bankApiConfig.getApiKey()).thenReturn("test-api-key");
        when(bankApiConfig.getClientId()).thenReturn("test-client-id");
    }

    @Test
    void processPayment_Success() throws Throwable {
        // Given
        when(retryTemplate.execute(any())).thenAnswer(invocation -> {
            return ResponseEntity.ok(successResponse);
        });

        // When
        PaymentResponse response = bankPaymentService.processPayment(paymentRequest);

        // Then
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("TXN1234567890", response.getPaymentId());
        assertEquals("Payment processed successfully", response.getMessage());
    }

    @Test
    void processPayment_Pending() throws Throwable {
        // Given
        when(retryTemplate.execute(any())).thenAnswer(invocation -> {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(pendingResponse);
        });

        // When
        PaymentResponse response = bankPaymentService.processPayment(paymentRequest);

        // Then
        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        assertEquals("TXN1234567890", response.getPaymentId());
    }

    @Test
    void processPayment_BankApiError() throws Throwable {
        // Given
        when(retryTemplate.execute(any()))
            .thenThrow(new BankApiException("Bank API error", HttpStatus.BAD_REQUEST));

        // When / Then
        assertThrows(PaymentProcessingException.class, () -> {
            bankPaymentService.processPayment(paymentRequest);
        });
    }

    @Test
    void processPayment_NetworkError() throws Throwable {
        // Given
        when(retryTemplate.execute(any()))
            .thenThrow(new ResourceAccessException("Connection timeout"));

        // When / Then
        assertThrows(PaymentProcessingException.class, () -> {
            bankPaymentService.processPayment(paymentRequest);
        });
    }

    @Test
    void getPaymentStatus_Success() throws Throwable {
        // Given
        String paymentId = "TXN1234567890";
        when(retryTemplate.execute(any())).thenAnswer(invocation -> {
            return ResponseEntity.ok(successResponse);
        });

        // When
        PaymentResponse response = bankPaymentService.getPaymentStatus(paymentId);

        // Then
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(paymentId, response.getPaymentId());
    }

    @Test
    void getPaymentStatus_NotFound() throws Throwable {
        // Given
        String paymentId = "NON_EXISTENT";
        when(retryTemplate.execute(any()))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Payment not found"));

        // When / Then
        assertThrows(PaymentProcessingException.class, () -> {
            bankPaymentService.getPaymentStatus(paymentId);
        });
    }

    @Test
    void getPaymentStatus_ServerError() throws Throwable {
        // Given
        String paymentId = "TXN1234567890";
        when(retryTemplate.execute(any()))
            .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server error"));

        // When / Then
        assertThrows(BankApiException.class, () -> {
            bankPaymentService.getPaymentStatus(paymentId);
        });
    }

    private BankPaymentResponseDTO createSuccessResponse() {
        return BankPaymentResponseDTO.builder()
                .success(true)
                .transactionId("TXN1234567890")
                .status("SUCCESS")
                .statusCode("SUCCESS")
                .statusDescription("Payment processed successfully")
                .build();
    }

    private BankPaymentResponseDTO createPendingResponse() {
        return BankPaymentResponseDTO.builder()
                .success(true)
                .transactionId("TXN1234567890")
                .status("PENDING")
                .statusCode("PENDING")
                .statusDescription("Payment is being processed")
                .build();
    }


}
