package com.shanthigear.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;

/**
 * Interceptor for rate limiting API requests.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String RATE_LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    private static final String RATE_LIMIT_RESET = "X-RateLimit-Reset";
    
    private final SecurityUtils securityUtils;
    private final long rateLimit;
    private final long rateLimitPeriod;

    @Autowired
    public RateLimitInterceptor(SecurityUtils securityUtils,
                               @Value("${app.security.rate-limit:100}") long rateLimit,
                               @Value("${app.security.rate-limit-period:3600}") long rateLimitPeriod) {
        this.securityUtils = securityUtils;
        this.rateLimit = rateLimit;
        this.rateLimitPeriod = rateLimitPeriod;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = securityUtils.getClientIpAddress().orElse("unknown");
        String endpoint = request.getRequestURI();
        String rateLimitKey = String.format("%s_%s", clientIp, endpoint);

        // Calculate remaining requests
        SecurityUtils.RequestRate rate = securityUtils.getRequestRate(rateLimitKey);
        long currentCount = rate != null ? rate.getRequestCount() : 0;
        long remaining = Math.max(0, rateLimit - currentCount);
        
        // Set rate limit headers
        response.setHeader(RATE_LIMIT_HEADER, String.valueOf(rateLimit));
        response.setHeader(RATE_LIMIT_REMAINING, String.valueOf(remaining));
        response.setHeader(RATE_LIMIT_RESET, String.valueOf(rateLimitPeriod));
        
        // Check if rate limit is exceeded
        if (securityUtils.isRateLimited(rateLimitKey)) {
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Rate limit exceeded. Please try again later.");
            return false;
        }

        return true;
    }
}
