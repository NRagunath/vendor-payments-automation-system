package com.shanthigear.service;

import org.springframework.beans.factory.annotation.Value;
import com.shanthigear.config.OracleH2HConfig;
import com.shanthigear.dto.PaymentRequestDTO;
import com.shanthigear.exception.BankApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.shanthigear.model.BankTransaction;

/**
 * Implementation of BankIntegrationService for real bank API integration.
 * Handles communication with the bank's payment processing API.
 */
@Service
public class BankIntegrationServiceImpl implements BankIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(BankIntegrationServiceImpl.class);
    
    private final RestTemplate restTemplate;
    private final OracleH2HConfig oracleH2HConfig;
    
    @Value("${oracle.h2h.api.timeout:30000}")
    private int timeoutMs;
    
    @Value("${oracle.h2h.api.transactions.endpoint:/api/v1/transactions}")
    private String transactionsEndpoint;
    
    public BankIntegrationServiceImpl(RestTemplate restTemplate, OracleH2HConfig oracleH2HConfig) {
        this.restTemplate = restTemplate;
        this.oracleH2HConfig = oracleH2HConfig;
        
        logger.info("Initialized BankIntegrationService with base URL: {}", oracleH2HConfig.getBaseUrl());
    }
    
    @Override
    @Retryable(retryFor = RestClientException.class, 
               maxAttemptsExpression = "${oracle.h2h.api.retry.attempts:3}",
               backoff = @Backoff(delayExpression = "${oracle.h2h.api.retry.delay:5000}",
                                maxDelayExpression = "${oracle.h2h.api.retry.max-delay:60000}",
                                multiplierExpression = "${oracle.h2h.api.retry.multiplier:2.0}"))
    public String processPayment(PaymentRequestDTO paymentRequest) throws BankApiException {
        if (paymentRequest == null) {
            throw new BankApiException("Payment request cannot be null", HttpStatus.BAD_REQUEST);
        }

        try {
            String url = oracleH2HConfig.getBaseUrl() + "/api/v1/payments";
            
            logger.info("Sending payment request to bank API: {}", url);
            
            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(oracleH2HConfig.getAuthToken());
            if (oracleH2HConfig.getClientId() != null && !oracleH2HConfig.getClientId().isEmpty()) {
                headers.set("X-Client-ID", oracleH2HConfig.getClientId());
            }
            headers.set("X-Request-ID", java.util.UUID.randomUUID().toString());
            
            // Create the request entity
            HttpEntity<PaymentRequestDTO> requestEntity = new HttpEntity<>(paymentRequest, headers);
            
            // Log request details (without sensitive data)
            if (logger.isDebugEnabled()) {
                logger.debug("Payment request - Amount: {}, Currency: {}, Reference: {}",
                           paymentRequest.getAmount(),
                           paymentRequest.getCurrency(),
                           paymentRequest.getPaymentReference());
            }
            
            // Make the API call
            ResponseEntity<String> response = restTemplate.exchange(
                URI.create(url),
                HttpMethod.POST,
                requestEntity,
                String.class
            );
            
            // Process the response
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Payment processed successfully. Reference: {}", paymentRequest.getPaymentReference());
                return response.getBody();
            } else {
                String errorMsg = String.format("Failed to process payment. Status: %s, Response: %s",
                                               response.getStatusCode().value(),
                                               response.getBody());
                logger.error(errorMsg);
                throw new BankApiException(errorMsg, response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            String errorMsg = String.format("Bank API client error - Status: %s, Response: %s",
                                          e.getStatusCode(),
                                          e.getResponseBodyAsString());
            logger.error(errorMsg, e);
            throw new BankApiException("Bank API request failed: " + e.getMessage(), e);
        } catch (RestClientException e) {
            String errorMsg = "Error communicating with bank API: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new BankApiException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Unexpected error processing payment: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new BankApiException(errorMsg, e);
        }
    }
    
    @Override
    public boolean verifyPayment(String paymentReference) throws BankApiException {
        if (paymentReference == null || paymentReference.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment reference cannot be null or empty");
        }

        String url = String.format("%s/api/payments/%s/status", oracleH2HConfig.getBaseUrl(), paymentReference);
        
        try {
            logger.info("Verifying payment status for reference: {}", paymentReference);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(oracleH2HConfig.getAuthToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null) {
                    String status = Objects.toString(responseBody.get("status"), "");
                    logger.info("Payment {} verification status: {}", paymentReference, status);
                    return "COMPLETED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status);
                }
            }
            
            logger.warn("Payment verification failed for reference: {}. Status: {}", 
                       paymentReference, response.getStatusCode());
            return false;
            
        } catch (HttpClientErrorException.NotFound e) {
            logger.error("Payment not found with reference: {}", paymentReference);
            throw new BankApiException("Payment not found: " + paymentReference, HttpStatus.NOT_FOUND);
        } catch (RestClientException e) {
            String errorMsg = "Error communicating with bank API during verification: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new BankApiException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Unexpected error verifying payment: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new BankApiException(errorMsg, e);
        }
    }
    
    @Override
    @Retryable(retryFor = RestClientException.class, 
              maxAttemptsExpression = "${oracle.h2h.api.retry.attempts:3}",
              backoff = @Backoff(delayExpression = "${oracle.h2h.api.retry.delay:5000}",
                               maxDelayExpression = "${oracle.h2h.api.retry.max-delay:60000}",
                               multiplierExpression = "${oracle.h2h.api.retry.multiplier:2.0}"))
    public List<BankTransaction> getBankTransactions(LocalDate fromDate, LocalDate toDate) throws BankApiException {
        logger.info("Fetching bank transactions from {} to {}", fromDate, toDate);
        
        try {
            // Validate input parameters
            if (fromDate == null || toDate == null) {
                throw new IllegalArgumentException("Both fromDate and toDate must be provided");
            }
            if (fromDate.isAfter(toDate)) {
                throw new IllegalArgumentException("fromDate cannot be after toDate");
            }
            
            // Build the request URL with query parameters
            String url = String.format("%s%s?fromDate=%s&toDate=%s", 
                oracleH2HConfig.getBaseUrl(),
                transactionsEndpoint, 
                fromDate.toString(),
                toDate.toString());
                
            logger.debug("Making API request to: {}", url);
            
            // Set up request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(oracleH2HConfig.getAuthToken());
            
            // Create request entity with headers
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            // Configure the rest template with timeout settings
            Timeout timeout = Timeout.ofMilliseconds(timeoutMs);
            RequestConfig config = RequestConfig.custom()
                .setConnectionRequestTimeout(timeout)
                .setResponseTimeout(timeout)
                .build();
                
            // Set connect timeout separately using ConnectionConfig
            ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(timeout)
                .build();
                
            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
            connectionManager.setMaxTotal(100);
            connectionManager.setDefaultMaxPerRoute(20);
            connectionManager.setDefaultConnectionConfig(connectionConfig);
            
            var httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(config)
                .build();
                
            var requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
            restTemplate.setRequestFactory(requestFactory);
            
            // Make the API call
            ParameterizedTypeReference<List<BankTransaction>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<List<BankTransaction>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                responseType
            );
            
            // Check response status
            if (response.getStatusCode().is2xxSuccessful()) {
                List<BankTransaction> transactions = response.getBody();
                if (transactions != null) {
                    logger.info("Successfully retrieved {} transactions from bank API", transactions.size());
                    return transactions;
                } else {
                    throw new BankApiException("Failed to fetch transactions. Status: " + response.getStatusCode(), response.getStatusCode());
                }
            } else {
                throw new BankApiException("Failed to fetch transactions. Status: " + response.getStatusCode(), response.getStatusCode());
            }
            
        } catch (HttpClientErrorException e) {
            String errorMsg = String.format("Client error while fetching transactions. Status: %s, Response: %s", 
                e.getStatusCode(), e.getResponseBodyAsString());
            logger.error(errorMsg, e);
            throw new BankApiException(errorMsg, e);
            
        } catch (RestClientException e) {
            String errorMsg = "Error communicating with bank API: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new BankApiException(errorMsg, e);
            
        } catch (Exception e) {
            String errorMsg = "Unexpected error while fetching transactions: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new BankApiException(errorMsg, e);
        }
    }
}
