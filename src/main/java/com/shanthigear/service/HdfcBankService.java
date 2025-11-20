package com.shanthigear.service;

import org.springframework.beans.factory.annotation.Value;
import com.shanthigear.exception.BankIntegrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.Map;

/**
 * Service for interacting with HDFC Bank's API for payment processing.
 */
@Service
public class HdfcBankService {
    private static final Logger logger = LoggerFactory.getLogger(HdfcBankService.class);
    private final RestTemplate restTemplate;
    @Value("${hdfc.bank.api.base-url}")
    private String baseUrl;

    @Value("${hdfc.bank.api.auth-token}")
    private String authToken;
    
    @Value("${hdfc.bank.api.client-id}")
    private String clientId;
    
    @Value("${hdfc.bank.api.timeout:30000}")
    private int timeoutMs;

    public HdfcBankService(@Qualifier("hdfcBankRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Process a payment through HDFC Bank API.
     * @param paymentDetails Map containing payment details including:
     *                      - amount (required): Payment amount
     *                      - beneficiaryAccount (required): Beneficiary account number
     *                      - beneficiaryName (required): Beneficiary name
     *                      - beneficiaryIFSC (required): Beneficiary IFSC code
     *                      - paymentReference (required): Unique payment reference
     *                      - paymentNarration (optional): Payment description
     * @return Map containing payment response with transaction details
     */
    @Retryable(
        retryFor = {HttpServerErrorException.class},
        maxAttemptsExpression = "${bank.api.max-retries:3}",
        backoff = @Backoff(delayExpression = "${bank.api.retry-delay:1000}")
    )
    public Map<String, Object> processPayment(Map<String, Object> paymentDetails) {
        try {
            validatePaymentDetails(paymentDetails);
            
            String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/api/v1/payments")
                .build()
                .toUriString();

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(paymentDetails, headers);

            logger.info("Sending payment request to HDFC Bank");
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                requestEntity, 
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            logger.info("Received response from HDFC Bank");
            return response.getBody();

        } catch (HttpClientErrorException e) {
            String errorMsg = String.format("Client error processing payment: %s - %s", 
                e.getStatusCode(), e.getResponseBodyAsString());
            logger.error(errorMsg, e);
            throw new BankIntegrationException(errorMsg, e);
        } catch (HttpServerErrorException e) {
            String errorMsg = String.format("Server error processing payment: %s - %s", 
                e.getStatusCode(), e.getResponseBodyAsString());
            logger.error(errorMsg, e);
            throw new BankIntegrationException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Unexpected error processing payment";
            logger.error(errorMsg, e);
            throw new BankIntegrationException(errorMsg, e);
        }
    }

    /**
     * Verify a payment status through HDFC Bank API.
     * @param transactionId The unique transaction ID to verify
     * @return Map containing verification details
     */
    @Retryable(
        retryFor = {HttpServerErrorException.class},
        maxAttemptsExpression = "${bank.api.max-retries:3}",
        backoff = @Backoff(delayExpression = "${bank.api.retry-delay:1000}")
    )
    public Map<String, Object> verifyPayment(String transactionId) {
        try {
            if (transactionId == null || transactionId.trim().isEmpty()) {
                throw new IllegalArgumentException("Transaction ID cannot be null or empty");
            }
            
            String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/api/v1/payments/" + transactionId)
                .build()
                .toUriString();

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            logger.info("Verifying payment status for transaction ID: {}", transactionId);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            logger.info("Received verification response for transaction ID: {}", transactionId);
            return response.getBody();

        } catch (HttpClientErrorException e) {
            String errorMsg = String.format("Client error verifying payment: %s - %s", 
                e.getStatusCode(), e.getResponseBodyAsString());
            logger.error(errorMsg, e);
            throw new BankIntegrationException(errorMsg, e);
        } catch (HttpServerErrorException e) {
            String errorMsg = String.format("Server error verifying payment: %s - %s", 
                e.getStatusCode(), e.getResponseBodyAsString());
            logger.error(errorMsg, e);
            throw new BankIntegrationException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Unexpected error verifying payment";
            logger.error(errorMsg, e);
            throw new BankIntegrationException(errorMsg, e);
        }
    }

    private void validatePaymentDetails(Map<String, ?> paymentDetails) {
        if (paymentDetails == null) {
            throw new IllegalArgumentException("Payment details cannot be null");
        }
        
        // Add validation for required fields
        String[] requiredFields = {"amount", "beneficiaryAccount", "beneficiaryName", "beneficiaryIFSC", "paymentReference"};
        for (String field : requiredFields) {
            if (!paymentDetails.containsKey(field) || paymentDetails.get(field) == null) {
                throw new IllegalArgumentException("Missing required field: " + field);
            }
        }
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + authToken);
        if (clientId != null && !clientId.isEmpty()) {
            headers.set("X-Client-ID", clientId);
        }
        return headers;
    }
}
