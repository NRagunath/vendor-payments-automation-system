package com.shanthigear.bank;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for interacting with the bank's payment API.
 * This is a template that needs to be implemented based on your bank's specific API.
 */
public class BankApiClient {
    private static final Logger logger = LoggerFactory.getLogger(BankApiClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // These should be moved to application.properties
    private final String apiBaseUrl = "https://api.yourbank.com/v1";
    private final String apiKey = "your-api-key";
    private final String clientId = "your-client-id";
    
    /**
     * Initiates a payment through the bank's API.
     */
    public String initiatePayment(String beneficiaryAccount, double amount, String paymentReference) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(apiBaseUrl + "/payments");
            
            // Set headers
            addCommonHeaders(request);
            
            // Prepare request body
            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("beneficiaryAccount", beneficiaryAccount);
            paymentRequest.put("amount", amount);
            paymentRequest.put("currency", "INR");
            paymentRequest.put("reference", paymentReference);
            paymentRequest.put("paymentType", "IMMEDIATE");
            
            String jsonBody = objectMapper.writeValueAsString(paymentRequest);
            request.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
            
            // Execute request
            return httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                
                if (statusCode == 200) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                    Object transactionId = responseMap.get("transactionId");
                    if (transactionId == null) {
                        throw new RuntimeException("No transaction ID in response");
                    }
                    return transactionId.toString();
                } else {
                    logger.error("Failed to initiate payment. Status: {}, Response: {}", statusCode, responseBody);
                    throw new RuntimeException("Failed to initiate payment: " + responseBody);
                }
            });
        } catch (Exception e) {
            logger.error("Error initiating payment", e);
            throw new RuntimeException("Error initiating payment", e);
        }
    }
    
    /**
     * Adds common headers to the HTTP request.
     * @param request The HTTP request to add headers to
     */
    private void addCommonHeaders(org.apache.hc.core5.http.ClassicHttpRequest request) {
        request.setHeader("Authorization", "Bearer " + apiKey);
        request.setHeader("Content-Type", "application/json");
        request.setHeader("X-Client-ID", clientId);
    }

    /**
     * Checks the status of a payment.
     */
    public BankPaymentStatus checkPaymentStatus(String transactionId) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(apiBaseUrl + "/payments/" + transactionId);
            addCommonHeaders(request);
            
            // Execute request
            return httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                
                if (statusCode == 200) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> statusMap = objectMapper.readValue(responseBody, Map.class);
                    Object statusObj = statusMap.get("status");
                    if (statusObj == null) {
                        throw new RuntimeException("No status in response");
                    }
                    return BankPaymentStatus.fromString(statusObj.toString());
                } else {
                    logger.error("Failed to check payment status. Status: {}, Response: {}", statusCode, responseBody);
                    throw new RuntimeException("Failed to check payment status: " + responseBody);
                }
            });
        } catch (Exception e) {
            logger.error("Error checking payment status", e);
            throw new RuntimeException("Error checking payment status", e);
        }
    }
}
