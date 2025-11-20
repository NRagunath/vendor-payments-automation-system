package com.shanthigear.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Interceptor for logging HTTP requests and responses.
 */
public class LoggingRequestInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingRequestInterceptor.class);
    private static final String REQUEST_LOG = "Request: {} {}";
    private static final String RESPONSE_LOG = "Response: {} {} - {} - {} ms";
    private static final String REQUEST_BODY_LOG = "Request body: {}";
    private static final String RESPONSE_BODY_LOG = "Response body: {}";

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        
        // Log request
        long startTime = System.currentTimeMillis();
        logRequest(request, body);
        
        // Execute the request
        ClientHttpResponse response = execution.execute(request, body);
        
        // Log response
        long duration = System.currentTimeMillis() - startTime;
        logResponse(request, response, duration);
        
        return response;
    }
    
    private void logRequest(HttpRequest request, byte[] body) {
        if (log.isDebugEnabled()) {
            log.debug(REQUEST_LOG, request.getMethod(), request.getURI());
            log.debug(REQUEST_BODY_LOG, new String(body, StandardCharsets.UTF_8));
        } else if (log.isInfoEnabled()) {
            log.info(REQUEST_LOG, request.getMethod(), request.getURI());
        }
    }
    
    private void logResponse(HttpRequest request, ClientHttpResponse response, long duration) throws IOException {
        if (log.isDebugEnabled()) {
            String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
            log.debug(RESPONSE_LOG, request.getMethod(), request.getURI(), 
                     response.getStatusCode(), duration);
            log.debug(RESPONSE_BODY_LOG, responseBody);
        } else if (log.isInfoEnabled()) {
            log.info(RESPONSE_LOG, request.getMethod(), request.getURI(), 
                    response.getStatusCode(), duration);
        }
    }
}
