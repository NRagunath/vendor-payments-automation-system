package com.shanthigear.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.beans.factory.annotation.Value;
import java.util.concurrent.ThreadPoolExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for the email service.
 */
@Configuration
@EnableAsync
public class EmailServiceConfig {

    /**
     * Configures the async executor for email sending.
     */
    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor(
            @Value("${app.email.executor.core-pool-size:5}") int corePoolSize,
            @Value("${app.email.executor.max-pool-size:20}") int maxPoolSize,
            @Value("${app.email.executor.queue-capacity:1000}") int queueCapacity) {
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("email-sender-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    
    /**
     * Configuration properties for email service.
     */
    @ConfigurationProperties(prefix = "app.email")
    @Bean
    public EmailProperties emailProperties() {
        return new EmailProperties();
    }
    
    /**
     * Email service properties.
     */
    public static class EmailProperties {
        private ExecutorProperties executor = new ExecutorProperties();
        private RateLimitProperties rateLimit = new RateLimitProperties();
        private CacheProperties cache = new CacheProperties();
        
        // Getters and setters
        public ExecutorProperties getExecutor() {
            return executor;
        }
        
        public void setExecutor(ExecutorProperties executor) {
            this.executor = executor;
        }
        
        public RateLimitProperties getRateLimit() {
            return rateLimit;
        }
        
        public void setRateLimit(RateLimitProperties rateLimit) {
            this.rateLimit = rateLimit;
        }
        
        public CacheProperties getCache() {
            return cache;
        }
        
        public void setCache(CacheProperties cache) {
            this.cache = cache;
        }
    }
    
    /**
     * Executor properties for email service.
     */
    public static class ExecutorProperties {
        private int corePoolSize = 5;
        private int maxPoolSize = 20;
        private int queueCapacity = 1000;
        
        // Getters and setters
        public int getCorePoolSize() {
            return corePoolSize;
        }
        
        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }
        
        public int getMaxPoolSize() {
            return maxPoolSize;
        }
        
        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }
        
        public int getQueueCapacity() {
            return queueCapacity;
        }
        
        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }
    
    /**
     * Rate limiting properties.
     */
    public static class RateLimitProperties {
        private int requestsPerSecond = 100;
        private int burstCapacity = 200;
        private String timeWindow = "1s";
        
        // Getters and setters
        public int getRequestsPerSecond() {
            return requestsPerSecond;
        }
        
        public void setRequestsPerSecond(int requestsPerSecond) {
            this.requestsPerSecond = requestsPerSecond;
        }
        
        public int getBurstCapacity() {
            return burstCapacity;
        }
        
        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }
        
        public String getTimeWindow() {
            return timeWindow;
        }
        
        public void setTimeWindow(String timeWindow) {
            this.timeWindow = timeWindow;
        }
    }
    
    /**
     * Cache properties for email senders.
     */
    public static class CacheProperties {
        private int maxSize = 1000;
        private int evictionBatch = 100;
        private boolean cleanupEnabled = true;
        private long cleanupInterval = 3600000; // 1 hour in milliseconds
        
        // Getters and setters
        public int getMaxSize() {
            return maxSize;
        }
        
        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }
        
        public int getEvictionBatch() {
            return evictionBatch;
        }
        
        public void setEvictionBatch(int evictionBatch) {
            this.evictionBatch = evictionBatch;
        }
        
        public boolean isCleanupEnabled() {
            return cleanupEnabled;
        }
        
        public void setCleanupEnabled(boolean cleanupEnabled) {
            this.cleanupEnabled = cleanupEnabled;
        }
        
        public long getCleanupInterval() {
            return cleanupInterval;
        }
        
        public void setCleanupInterval(long cleanupInterval) {
            this.cleanupInterval = cleanupInterval;
        }
    }
}
