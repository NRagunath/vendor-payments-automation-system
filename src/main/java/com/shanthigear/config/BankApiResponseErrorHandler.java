package com.shanthigear.config;

import com.shanthigear.exception.BankApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Handles errors from bank API responses.
 */
@Component
public class BankApiResponseErrorHandler implements ResponseErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(BankApiResponseErrorHandler.class);
    private static final String ERROR_MESSAGE = "Bank API request failed with status code: %d - %s";

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().isError();
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        HttpStatusCode statusCode = response.getStatusCode();
        String statusText = response.getStatusText();
        
        log.error("Bank API error - Status: {} {}, Response: {}", 
                statusCode, statusText, responseBody);
        
        String errorMessage = String.format(ERROR_MESSAGE, statusCode.value(), statusText);
        
        // Map HTTP status to appropriate exception
        if (statusCode.value() == HttpStatus.BAD_REQUEST.value()) {
            throw new BankApiException("Invalid request: " + errorMessage, statusCode);
        } else if (statusCode.value() == HttpStatus.UNAUTHORIZED.value()) {
            throw new BankApiException("Authentication failed: " + errorMessage, statusCode);
        } else if (statusCode.value() == HttpStatus.FORBIDDEN.value()) {
            throw new BankApiException("Insufficient permissions: " + errorMessage, statusCode);
        } else if (statusCode.value() == HttpStatus.NOT_FOUND.value()) {
            throw new BankApiException("Resource not found: " + errorMessage, statusCode);
        } else if (statusCode.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
            throw new BankApiException("Rate limit exceeded: " + errorMessage, statusCode);
        } else if (statusCode.value() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            throw new BankApiException("Bank server error: " + errorMessage, statusCode);
        } else if (statusCode.value() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
            throw new BankApiException("Bank service unavailable: " + errorMessage, statusCode);
        } else {
            throw new BankApiException("Bank API error: " + errorMessage, statusCode);
        }
    }
}
