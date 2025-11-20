package com.shanthigear.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.shanthigear.config.EmailConfig;
import com.shanthigear.config.EmailSenderConfigurator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Factory for creating and caching JavaMailSender instances based on email domains.
 */
@Component
public class EmailSenderFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailSenderFactory.class);
    
    private static final int DEFAULT_MAX_CACHE_SIZE = 1000;
    private static final int DEFAULT_EVICTION_BATCH = 100;
    private static final long CACHE_EXPIRE_AFTER_WRITE_MINUTES = 60; // 1 hour
    private static final long CACHE_REFRESH_AFTER_WRITE_MINUTES = 30; // 30 minutes
    
    private final int maxCacheSize;
    // Using Caffeine cache for better performance and eviction policies
    private final Cache<String, JavaMailSender> mailSenders;
    // EmailConfig is kept for potential future use
    private final EmailSenderConfigurator configurator;
    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeSenders = new AtomicInteger(0);
    private final Timer cacheLoadTimer;
    
    public EmailSenderFactory(EmailConfig emailConfig, 
                            EmailSenderConfigurator configurator,
                            MeterRegistry meterRegistry) {
        // emailConfig parameter is kept for future use
        this(emailConfig, configurator, meterRegistry, 
             DEFAULT_MAX_CACHE_SIZE, DEFAULT_EVICTION_BATCH);
    }
    
    public EmailSenderFactory(EmailConfig emailConfig,
                            EmailSenderConfigurator configurator,
                            MeterRegistry meterRegistry,
                            int maxCacheSize,
                            int evictionBatchSize) {
        // emailConfig parameter is kept for future use
        this.configurator = configurator;
        this.meterRegistry = meterRegistry;
        this.maxCacheSize = maxCacheSize > 0 ? maxCacheSize : DEFAULT_MAX_CACHE_SIZE;
        // Removed unused variable as it's not needed
        
        // Initialize cache with eviction and refresh policies
        this.mailSenders = Caffeine.newBuilder()
            .maximumSize(this.maxCacheSize)
            .expireAfterWrite(CACHE_EXPIRE_AFTER_WRITE_MINUTES, TimeUnit.MINUTES)
            .refreshAfterWrite(CACHE_REFRESH_AFTER_WRITE_MINUTES, TimeUnit.MINUTES)
            .recordStats()
            .removalListener((String key, JavaMailSender sender, RemovalCause cause) -> {
                if (key != null && !key.equals("default")) {
                    activeSenders.decrementAndGet();
                    logger.debug("Removed mail sender from cache: {} (cause: {})", key, cause);
                }
            })
            .build(this::createMailSender);
            
        // Initialize with default mail sender
        this.mailSenders.put("default", configurator.createMailSender(""));
        activeSenders.incrementAndGet();
        
        // Setup metrics
        this.cacheLoadTimer = Timer.builder("email.sender.cache.load.time")
            .description("Time taken to load/create mail senders")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
            
        // Register metrics
        registerMetrics();
    }
    
    private void registerMetrics() {
        // Cache metrics
        meterRegistry.gauge("email.senders.cache.size", 
            Tags.empty(),
            this.mailSenders.estimatedSize());
            
        meterRegistry.gauge("email.senders.active", 
            Tags.empty(),
            this.activeSenders);
            
        // Cache hit/miss rates using the cache's stats
        meterRegistry.gauge("email.senders.cache.hit.ratio", 
            Tags.of("type", "hit"),
            this.mailSenders.stats().hitRate());
            
        meterRegistry.gauge("email.senders.cache.miss.ratio", 
            Tags.of("type", "miss"),
            this.mailSenders.stats().missRate());
            
        // Eviction metrics
        meterRegistry.gauge("email.senders.cache.eviction.count",
            Tags.empty(),
            this.mailSenders.stats().evictionCount());
            
        // Load metrics
        meterRegistry.gauge("email.senders.cache.load.success.count", 
            Tags.empty(),
            this.mailSenders.stats().loadSuccessCount());
            
        meterRegistry.gauge("email.senders.cache.load.failure.count",
            Tags.empty(),
            this.mailSenders.stats().loadFailureCount());
            
        // Average load time
        meterRegistry.gauge("email.senders.cache.load.average.time",
            Tags.empty(),
            this.mailSenders.stats().averageLoadPenalty() / 1_000_000); // Convert to ms
    }
    
    /**
     * Get a JavaMailSender for the given email address.
     * @param email The email address to get a sender for
     * @return A configured JavaMailSender instance
     */
    public JavaMailSender getMailSender(String email) {
        if (email == null || email.isEmpty()) {
            return mailSenders.getIfPresent("default");
        }
        
        String domain = configurator.extractDomain(email);
        if (domain.isEmpty()) {
            return mailSenders.getIfPresent("default");
        }
        
        try {
            // Use the cache's get method which will trigger loading if necessary
            return cacheLoadTimer.record(() -> 
                mailSenders.get(domain, this::createMailSender));
        } catch (Exception e) {
            logger.error("Failed to get or create mail sender for domain: " + domain, e);
            return mailSenders.getIfPresent("default");
        }
    }
    
    /**
     * Create a new mail sender for the given domain.
     * This method is called by the cache when a sender is not found.
     */
    private JavaMailSender createMailSender(String domain) {
        logger.info("Creating new mail sender for domain: {}", domain);
        try {
            JavaMailSender sender = configurator.createMailSender(domain);
            activeSenders.incrementAndGet();
            meterRegistry.counter("email.sender.created", "domain", domain).increment();
            return sender;
        } catch (Exception e) {
            logger.error("Failed to create mail sender for domain: " + domain, e);
            throw e;
        }
    }
    
    /**
     * Clear the mail sender cache, keeping only the default sender.
     * @return The number of senders that were removed
     */
    public long clearCache() {
        logger.info("Clearing mail sender cache");
        long count = mailSenders.estimatedSize() - 1; // Exclude default sender
        
        // Get all keys except 'default'
        Set<String> keysToRemove = mailSenders.asMap().keySet().stream()
            .filter(key -> !"default".equals(key))
            .collect(Collectors.toSet());
            
        // Remove all keys
        mailSenders.invalidateAll(keysToRemove);
        
        meterRegistry.counter("email.sender.cache.cleared").increment();
        logger.info("Cleared {} mail senders from cache, kept default sender", count);
        
        return count;
    }
    
    /**
     * Preload mail senders for the given domains.
     * @param domains The domains to preload
     */
    public void preloadMailSenders(Collection<String> domains) {
        if (domains == null || domains.isEmpty()) {
            return;
        }
        
        logger.info("Preloading mail senders for {} domains", domains.size());
        
        domains.parallelStream()
            .filter(domain -> domain != null && !domain.isEmpty())
            .forEach(domain -> {
                try {
                    mailSenders.get(domain, this::createMailSender);
                } catch (Exception e) {
                    logger.warn("Failed to preload mail sender for domain: " + domain, e);
                }
            });
    }
    
    /**
     * Get the current number of cached mail senders.
     * @return The estimated number of cached mail senders (including the default sender)
     */
    public long getCacheSize() {
        return mailSenders.estimatedSize();
    }
    
    /**
     * Get cache statistics.
     * @return The cache statistics
     */
    public com.github.benmanes.caffeine.cache.stats.CacheStats getCacheStats() {
        return mailSenders.stats();
    }
    
    /**
     * Get the current cache hit rate.
     * @return The cache hit rate as a value between 0 and 1
     */
    
    /**
     * Get the current cache hit rate.
     * @return The cache hit rate as a value between 0 and 1
     */
    public double getCacheHitRate() {
        return mailSenders.stats().hitRate();
    }
    
    /**
     * Get the current cache miss rate.
     * @return The cache miss rate as a value between 0 and 1
     */
    public double getCacheMissRate() {
        return mailSenders.stats().missRate();
    }
}
