package com.shanthigear.config;

import com.shanthigear.service.EmailSenderFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration for scheduled tasks.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {

    private final EmailSenderFactory emailSenderFactory;
    private final boolean enableCacheCleanup;
    private final long cacheCleanupIntervalMs;

    public SchedulerConfig(EmailSenderFactory emailSenderFactory,
                          @Value("${app.email.cache.cleanup.enabled:true}") boolean enableCacheCleanup,
                          @Value("${app.email.cache.cleanup.interval:3600000}") long cacheCleanupIntervalMs) {
        this.emailSenderFactory = emailSenderFactory;
        this.enableCacheCleanup = enableCacheCleanup;
        this.cacheCleanupIntervalMs = cacheCleanupIntervalMs;
    }
    
    /**
     * @return The interval in milliseconds at which the cache cleanup should run
     */
    public long getCacheCleanupIntervalMs() {
        return cacheCleanupIntervalMs;
    }

    /**
     * Scheduled task to clean up the email sender cache at a fixed rate.
     * This helps in preventing memory leaks by removing unused senders.
     */
    @Scheduled(fixedRateString = "#{@schedulerConfig.getCacheCleanupIntervalMs()}")
    public void cleanupEmailSendersCache() {
        if (enableCacheCleanup) {
            try {
                emailSenderFactory.clearCache();
            } catch (Exception e) {
                // Log the error but don't fail the scheduled task
                org.slf4j.LoggerFactory.getLogger(SchedulerConfig.class)
                    .error("Error cleaning up email sender cache: {}", e.getMessage(), e);
            }
        }
    }
}
