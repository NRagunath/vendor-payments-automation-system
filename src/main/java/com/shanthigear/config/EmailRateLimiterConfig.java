package com.shanthigear.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

import java.time.Duration;
import java.util.function.Function;

/**
 * Configuration for rate limiting email sending operations using Resilience4j.
 */
@Configuration
public class EmailRateLimiterConfig {
    private static final Logger log = LoggerFactory.getLogger(EmailRateLimiterConfig.class);

    @Value("${app.email.rate-limit.requests-per-second:100}")
    private int requestsPerSecond;

    @Value("${app.email.rate-limit.burst-capacity:200}")
    private int burstCapacity;

    @Value("${app.email.rate-limit.time-window:1s}")
    private String timeWindow;

    /**
     * Creates a rate limiter registry with custom configuration.
     * @return Configured RateLimiterRegistry
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        Duration window = Duration.parse("PT" + timeWindow);
        
        // Configure default rate limiter settings
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(requestsPerSecond)
            .limitRefreshPeriod(window)
            .timeoutDuration(Duration.ZERO) // Don't wait for permission, fail immediately if limit is exceeded
            .build();
            
        return RateLimiterRegistry.of(config);
    }
    
    /**
     * Creates a global rate limiter for email sending operations.
     * @param registry The rate limiter registry
     * @return A configured RateLimiter instance
     */
    @Bean
    public RateLimiter emailRateLimiter(RateLimiterRegistry registry) {
        RateLimiter rateLimiter = registry.rateLimiter("email-rate-limiter");
        
        // Add event consumers for monitoring
        rateLimiter.getEventPublisher()
            .onSuccess(event -> log.debug("Rate limiter success: {}", event))
            .onFailure(event -> log.debug("Rate limiter failure: {}", event));
            
        return rateLimiter;
    }
    
    /**
     * Creates a factory for domain-specific rate limiters.
     * @param registry The rate limiter registry
     * @return A function that provides a RateLimiter per domain
     */
    @Bean
    public Function<String, RateLimiter> domainRateLimiters(RateLimiterRegistry registry) {
        return domain -> {
            // Create a unique name for each domain's rate limiter
            String rateLimiterName = "domain-rate-limiter-" + domain;
            
            // Get or create a rate limiter for this domain
            RateLimiter rateLimiter = registry.rateLimiter(rateLimiterName);
            
            // Log rate limiter events for monitoring
            rateLimiter.getEventPublisher()
                .onSuccess(event -> log.debug("Domain rate limiter success for {}: {}", domain, event))
                .onFailure(event -> log.warn("Domain rate limiter failure for {}: {}", domain, event));
                
            return rateLimiter;
        };
    }
    
    /**
     * Customizer to configure rate limiter properties at runtime.
     * @return RateLimiterConfigCustomizer
     */
    @Bean
    public RateLimiterConfigCustomizer rateLimiterConfigCustomizer() {
        return new RateLimiterConfigCustomizer() {
            @Override
            public void customize(RateLimiterConfig.Builder builder) {
                // This will apply to all rate limiters
                builder.timeoutDuration(Duration.ofMillis(100));
            }
            
            @Override
            public String name() {
                return "emailRateLimiterCustomizer";
            }
        };
    }
    
    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;
    
    /**
     * Logs rate limiter events for monitoring and debugging.
     */
    @PostConstruct
    public void registerEventLogger() {
        rateLimiterRegistry.getEventPublisher()
            .onEntryAdded(entryAdded -> {
                entryAdded.getAddedEntry().getEventPublisher()
                    .onSuccess(event -> log.debug("Rate limiter success: {}", event.getRateLimiterName()))
                    .onFailure(event -> log.warn("Rate limiter failure: {}", event.getRateLimiterName()));
            });
    }
}
