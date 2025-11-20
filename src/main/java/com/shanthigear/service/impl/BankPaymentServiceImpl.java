package com.shanthigear.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanthigear.config.BankApiConfig;
import com.shanthigear.dto.BankPaymentRequestDTO;
import com.shanthigear.dto.BankPaymentResponseDTO;
import com.shanthigear.exception.BankApiException;
import com.shanthigear.exception.PaymentProcessingException;
import com.shanthigear.payload.request.PaymentRequest;
import com.shanthigear.payload.response.PaymentResponse;
import com.shanthigear.service.BankPaymentService;
import com.shanthigear.util.SecureLoggingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;

/**
 * Implementation of BankPaymentService for processing bank payments with retry and error handling.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BankPaymentServiceImpl implements BankPaymentService {

    @Qualifier("bankRestTemplate")
    private final RestTemplate restTemplate;
    private final BankApiConfig bankApiConfig;
    private final ObjectMapper objectMapper;
    private final RetryTemplate retryTemplate;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    
    @Override
    @Retryable(
        retryFor = { ResourceAccessException.class, HttpServerErrorException.class },
        maxAttemptsExpression = "${bank.api.retry.max-attempts:3}",
        backoff = @Backoff(delayExpression = "${bank.api.retry.initial-delay:1000}",
                          multiplierExpression = "${bank.api.retry.multiplier:2}")
    )
    public PaymentResponse processPayment(PaymentRequest paymentRequest) {
        try {
            // Log the payment request (masking sensitive data)
            SecureLoggingUtils.info(log, "Processing payment request for amount: {}, beneficiary: {}",
                paymentRequest.getAmount(), paymentRequest.getBeneficiaryAccountNumber());
            
            // Convert to bank-specific DTO
            BankPaymentRequestDTO bankRequest = convertToBankRequest(paymentRequest);
            
            // Build the request URL
            String url = String.format("%s%s", 
                bankApiConfig.getBaseUrl(), 
                bankApiConfig.getProcessPaymentEndpoint());
            
            // Set up headers with idempotency key
            HttpHeaders headers = createHeaders();
            headers.add(IDEMPOTENCY_KEY_HEADER, generateIdempotencyKey(paymentRequest));
            
            // Create request entity
            HttpEntity<BankPaymentRequestDTO> requestEntity = new HttpEntity<>(bankRequest, headers);
            
            // Log the request details (without sensitive data)
            if (log.isDebugEnabled()) {
                log.debug("Sending payment request to bank API: {}", url);
                log.debug("Request headers: {}", headers);
                log.debug("Request body (masked): {}", 
                    SecureLoggingUtils.maskSensitiveData(objectMapper.writeValueAsString(bankRequest)));
            }
            
            // Make the API call with retry template
            ResponseEntity<BankPaymentResponseDTO> response = retryTemplate.execute(context -> {
                try {
                    return restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        requestEntity,
                        BankPaymentResponseDTO.class
                    );
                } catch (HttpClientErrorException | HttpServerErrorException e) {
                    // Log the error response from the bank
                    String responseBody = e.getResponseBodyAsString();
                    log.error("Bank API error: {} - {}", e.getStatusCode(), responseBody);
                    throw new BankApiException(
                        String.format("Bank API error: %s - %s", e.getStatusCode(), responseBody),
                        e.getStatusCode()
                    );
                } catch (RestClientException e) {
                    log.error("Error calling bank API: {}", e.getMessage(), e);
                    throw new PaymentProcessingException("Error processing payment with bank", e);
                }
            });
            
            // Process the response
            return processBankResponse(response.getBody());
            
        } catch (Exception e) {
            SecureLoggingUtils.error(log, "Failed to process payment", e);
            throw new PaymentProcessingException("Failed to process payment: " + e.getMessage(), e);
        }
    }

    @Override
    @Retryable(
        retryFor = { ResourceAccessException.class, HttpServerErrorException.class },
        maxAttemptsExpression = "${bank.api.retry.max-attempts:3}",
        backoff = @Backoff(delayExpression = "${bank.api.retry.initial-delay:1000}",
                          multiplierExpression = "${bank.api.retry.multiplier:2}")
    )
    public PaymentResponse getPaymentStatus(String paymentId) {
        try {
            SecureLoggingUtils.info(log, "Fetching payment status for payment ID: {}", paymentId);
            
            // Validate payment ID
            if (paymentId == null || paymentId.trim().isEmpty()) {
                throw new IllegalArgumentException("Payment ID cannot be null or empty");
            }
            
            // Build the request URL with the payment ID
            String url = String.format("%s%s/%s", 
                bankApiConfig.getBaseUrl(), 
                bankApiConfig.getVerifyPaymentEndpoint(),
                URLEncoder.encode(paymentId, StandardCharsets.UTF_8.toString()));
            
            // Set up headers
            HttpHeaders headers = createHeaders();
            
            // Create request entity
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            // Log the request
            if (log.isDebugEnabled()) {
                log.debug("Fetching payment status from: {}", url);
            }
            
            // Make the API call with retry template
            ResponseEntity<BankPaymentResponseDTO> response = retryTemplate.execute(context -> {
                try {
                    return restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        requestEntity,
                        BankPaymentResponseDTO.class
                    );
                } catch (HttpClientErrorException.NotFound e) {
                    log.warn("Payment not found with ID: {}", paymentId);
                    throw new PaymentProcessingException("Payment not found with ID: " + paymentId, e);
                } catch (HttpClientErrorException | HttpServerErrorException e) {
                    String responseBody = e.getResponseBodyAsString();
                    log.error("Bank API error during status check: {} - {}", e.getStatusCode(), responseBody);
                    throw new BankApiException(
                        String.format("Bank API error: %s - %s", e.getStatusCode(), responseBody),
                        e.getStatusCode()
                    );
                } catch (RestClientException e) {
                    log.error("Error calling bank API for status check: {}", e.getMessage(), e);
                    throw new PaymentProcessingException("Error checking payment status with bank", e);
                }
            });
            
            // Process the response
            return processBankResponse(response.getBody());
            
        } catch (Exception e) {
            SecureLoggingUtils.error(log, "Failed to get payment status for ID: " + paymentId, e);
            throw new PaymentProcessingException("Failed to get payment status: " + e.getMessage(), e);
        }
    }
    
    /**
     * Converts the application's PaymentRequest to the bank's specific DTO.
     */
    private BankPaymentRequestDTO convertToBankRequest(PaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Payment request cannot be null");
        }
        
        return BankPaymentRequestDTO.builder()
            .transactionReference(generateTransactionReference())
            .debitAccountNumber(request.getDebitAccountNumber())
            .beneficiaryAccountNumber(request.getBeneficiaryAccountNumber())
            .beneficiaryName(request.getBeneficiaryName())
            .beneficiaryBankCode(request.getBeneficiaryBankCode())
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .valueDate(LocalDateTime.now().format(DATE_FORMATTER))
            .paymentDetails(request.getDescription())
            .endToEndId(UUID.randomUUID().toString())
            .build();
    }
    
    /**
     * Processes the bank's response and converts it to the application's PaymentResponse.
     */
    private PaymentResponse processBankResponse(BankPaymentResponseDTO bankResponse) {
        if (bankResponse == null) {
            throw new PaymentProcessingException("Received null response from bank");
        }
        
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(bankResponse.getTransactionId());
        response.setStatus(mapBankStatus(bankResponse.getStatus()));
        response.setMessage(bankResponse.getStatusDescription());
        response.setReference(bankResponse.getTransactionReference());
        response.setTimestamp(bankResponse.getCreatedAt() != null ? 
            bankResponse.getCreatedAt() : LocalDateTime.now());
            
        // Set additional fields if available in the response
        if (bankResponse.getAmount() != null) {
            response.setAmount(bankResponse.getAmount());
        }
        if (bankResponse.getCurrency() != null) {
            response.setCurrency(bankResponse.getCurrency());
        }
        
        // Log the response (masking sensitive data)
        if (log.isDebugEnabled()) {
            try {
                log.debug("Bank API response: {}", 
                    SecureLoggingUtils.maskSensitiveData(objectMapper.writeValueAsString(bankResponse)));
            } catch (Exception e) {
                log.warn("Failed to log bank response", e);
            }
        }
        
        return response;
    }
    
    /**
     * Maps the bank's status to the application's status.
     */
    private String mapBankStatus(String bankStatus) {
        if (bankStatus == null) {
            return "UNKNOWN";
        }
        
        return switch (bankStatus.toUpperCase()) {
            case "SUCCESS", "COMPLETED" -> "SUCCESS";
            case "PENDING", "IN_PROGRESS", "PROCESSING" -> "PENDING";
            case "FAILED", "REJECTED", "DECLINED", "CANCELLED" -> "FAILED";
            default -> bankStatus;
        };
    }
    
    /**
     * Creates HTTP headers with authentication and content type.
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        
        // Add authentication based on configuration
        if (StringUtils.isNotBlank(bankApiConfig.getApiKey())) {
            headers.set("X-API-Key", bankApiConfig.getApiKey());
            if (StringUtils.isNotBlank(bankApiConfig.getClientId())) {
                headers.set("X-Client-ID", bankApiConfig.getClientId());
            }
        } else if (StringUtils.isNotBlank(bankApiConfig.getAuthToken())) {
            headers.setBearerAuth(bankApiConfig.getAuthToken());
        } else if (StringUtils.isNotBlank(bankApiConfig.getUsername()) && 
                  StringUtils.isNotBlank(bankApiConfig.getPassword())) {
            String auth = bankApiConfig.getUsername() + ":" + bankApiConfig.getPassword();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);
        }
        
        // Add request ID for tracking
        headers.set("X-Request-ID", UUID.randomUUID().toString());
        
        // Add timestamp
        headers.set("X-Timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        
        return headers;
    }
    
    /**
     * Generates a unique transaction reference.
     */
    private String generateTransactionReference() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Generates an idempotency key for the payment request.
     */
    private String generateIdempotencyKey(PaymentRequest request) {
        String uniqueId = String.format("%s-%s-%s",
            request.getDebitAccountNumber(),
            request.getBeneficiaryAccountNumber(),
            request.getAmount().toString());
        
        return UUID.nameUUIDFromBytes(uniqueId.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
