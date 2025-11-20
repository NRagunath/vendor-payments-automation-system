package com.shanthigear.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for Spring Security with enhanced security features.
 */
@Component
public class SecurityUtils {
    private static final Logger logger = LoggerFactory.getLogger(SecurityUtils.class);
    private static final String[] IP_HEADER_CANDIDATES = {
        "X-Forwarded-For",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "REMOTE_ADDR"
    };

    // Rate limiting
    private final Map<String, RequestRate> requestRates = new ConcurrentHashMap<>();
    private final long rateLimit;
    private final long rateLimitPeriod;

    private final HttpServletRequest request;
    
    @Autowired
    public SecurityUtils(
            @Value("${app.security.rate-limit:100}") long rateLimit,
            @Value("${app.security.rate-limit-period:3600}") long rateLimitPeriod,
            HttpServletRequest request) {
        this.rateLimit = rateLimit;
        this.rateLimitPeriod = rateLimitPeriod;
        this.request = request;
    }



    /**
     * Get the current user ID from the security context.
     */
    public Optional<String> getCurrentUserId() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getPrincipal)
                .map(principal -> {
                    if (principal instanceof UserDetails) {
                        return ((UserDetails) principal).getUsername();
                    } else if (principal instanceof String) {
                        return (String) principal;
                    }
                    return null;
                });
    }

    /**
     * Get the client IP address from the request.
     */
    public Optional<String> getClientIpAddress() {
        if (request == null) {
            return Optional.of("127.0.0.1");
        }

        for (String header : IP_HEADER_CANDIDATES) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                return Optional.of(ip.split(",")[0]);
            }
        }
        return Optional.ofNullable(request.getRemoteAddr());
    }

    /**
     * Check if the current user is authenticated.
     */
    public boolean isAuthenticated() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::isAuthenticated)
                .orElse(false);
    }

    /**
     * Check if the current user has a specific authority/role.
     */
    public boolean hasAuthority(String authority) {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(grantedAuth -> grantedAuth.getAuthority().equals(authority)))
                .orElse(false);
    }

    /**
     * Check if the current request is rate limited.
     */
    public boolean isRateLimited(String identifier) {
        if (rateLimit <= 0) {
            return false;
        }

        String key = getClientIpAddress().orElse("anonymous") + "_" + identifier;
        RequestRate rate = requestRates.computeIfAbsent(key, k -> new RequestRate());
        
        synchronized (rate) {
            long now = System.currentTimeMillis();
            rate.requests.removeIf(timestamp -> now - timestamp > TimeUnit.SECONDS.toMillis(rateLimitPeriod));
            
            if (rate.requests.size() >= rateLimit) {
                logger.warn("Rate limit exceeded for {}", key);
                return true;
            }
            
            rate.requests.add(now);
            return false;
        }
    }

    /**
     * Generate a secure random token.
     */
    public String generateSecureToken() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Validate a CSRF token.
     */
    public boolean isValidCsrfToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        // Add your CSRF token validation logic here
        return true;
    }

    /**
     * Log security event.
     */
    public void logSecurityEvent(String event, String details) {
        logger.warn("Security Event - {}: {}", event, details);
        // Add audit logging here
    }

    /**
     * Get the current request rate for a given identifier.
     * @param identifier The identifier to get the request rate for
     * @return The RequestRate object containing the request timestamps, or null if not found
     */
    public RequestRate getRequestRate(String identifier) {
        String key = getClientIpAddress().orElse("anonymous") + "_" + identifier;
        return requestRates.get(key);
    }

    /**
     * Inner class to track request rates.
     */
    public static class RequestRate {
        final List<Long> requests = new ArrayList<>();
        
        /**
         * Get the number of requests in the current rate limit window.
         * @return The number of requests
         */
        public int getRequestCount() {
            return requests.size();
        }
    }

    /**
     * Get the current session ID.
     */
    public Optional<String> getSessionId() {
        return Optional.ofNullable(request)
                .map(req -> req.getSession(false))
                .map(session -> session.getId());
    }

    /**
     * Invalidate the current session.
     */
    public void invalidateSession() {
        Optional.ofNullable(request)
                .map(req -> req.getSession(false))
                .ifPresent(session -> session.invalidate());
    }

    /**
     * Check if the request is coming from a secure channel (HTTPS).
     */
    public boolean isSecure() {
        return request != null && request.isSecure();
    }

    /**
     * Get the user agent from the request.
     */
    public Optional<String> getUserAgent() {
        return Optional.ofNullable(request)
                .map(req -> req.getHeader("User-Agent"));
    }
}
