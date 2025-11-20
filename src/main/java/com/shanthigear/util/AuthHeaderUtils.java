package com.shanthigear.util;

import com.shanthigear.config.BankApiConfig;
import org.apache.hc.core5.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.util.Base64;
import java.util.Collections;

/**
 * Utility class for handling authentication headers for bank API requests.
 */
@Component
public class AuthHeaderUtils {

    private static final String AUTH_HEADER_PREFIX = "Bearer ";
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String CLIENT_ID_HEADER = "X-Client-ID";
    
    private final BankApiConfig bankApiConfig;
    
    public AuthHeaderUtils(BankApiConfig bankApiConfig) {
        this.bankApiConfig = bankApiConfig;
    }
    
    /**
     * Creates an interceptor that adds the necessary authentication headers to outgoing requests.
     * @return ClientHttpRequestInterceptor that adds authentication headers
     */
    public ClientHttpRequestInterceptor createAuthInterceptor() {
        return (request, body, execution) -> {
            addAuthHeaders(request);
            return execution.execute(request, body);
        };
    }
    
    /**
     * Adds authentication headers to the HTTP request.
     * @param request The HTTP request to add headers to
     */
    public void addAuthHeaders(HttpRequest request) {
        // Add API key if configured
        if (StringUtils.hasText(bankApiConfig.getApiKey())) {
            request.getHeaders().set(API_KEY_HEADER, bankApiConfig.getApiKey());
        }
        
        // Add Basic Auth if username/password is configured
        if (StringUtils.hasText(bankApiConfig.getUsername()) && 
            StringUtils.hasText(bankApiConfig.getPassword())) {
            
            String auth = bankApiConfig.getUsername() + ":" + bankApiConfig.getPassword();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
        }
        
        // Add Bearer token if configured
        if (StringUtils.hasText(bankApiConfig.getAuthToken())) {
            request.getHeaders().set(
                HttpHeaders.AUTHORIZATION, 
                AUTH_HEADER_PREFIX + bankApiConfig.getAuthToken()
            );
        }
        
        // Add client ID if configured
        if (StringUtils.hasText(bankApiConfig.getClientId())) {
            request.getHeaders().set(CLIENT_ID_HEADER, bankApiConfig.getClientId());
        }
        
        // Ensure we're sending JSON
        request.getHeaders().setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    }
    
    /**
     * Validates that the required authentication configuration is present.
     * @throws IllegalStateException if required configuration is missing
     */
    public void validateAuthConfig() {
        boolean hasApiKey = StringUtils.hasText(bankApiConfig.getApiKey());
        boolean hasBasicAuth = StringUtils.hasText(bankApiConfig.getUsername()) && 
                              StringUtils.hasText(bankApiConfig.getPassword());
        boolean hasBearerToken = StringUtils.hasText(bankApiConfig.getAuthToken());
        
        if (!hasApiKey && !hasBasicAuth && !hasBearerToken) {
            throw new IllegalStateException(
                "No authentication method configured for bank API. " +
                "Please configure at least one of: api-key, basic auth (username/password), or bearer token"
            );
        }
    }
}
