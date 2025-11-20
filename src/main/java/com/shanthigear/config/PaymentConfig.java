package com.shanthigear.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration class for payment processing.
 * Sets up payment-related configurations and async executors.
 */
@Configuration
@EnableAsync
public class PaymentConfig {

    @Value("${payment.max-retry-attempts:3}")
    private int maxRetryAttempts;

    @Value("${payment.retry-delay-ms:5000}")
    private long retryDelayMs;

    @Value("${payment.async.core-pool-size:5}")
    private int corePoolSize;

    @Value("${payment.async.max-pool-size:20}")
    private int maxPoolSize;

    @Value("${payment.async.queue-capacity:1000}")
    private int queueCapacity;

    /**
     * Configures an async executor for payment processing tasks.
     * @return Configured ThreadPoolTaskExecutor
     */
    @Bean(name = "paymentTaskExecutor")
    public Executor paymentTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("PaymentProcessor-");
        executor.initialize();
        return executor;
    }

    /**
     * @return Maximum number of retry attempts for failed payments
     */
    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    /**
     * @return Delay between retry attempts in milliseconds
     */
    public long getRetryDelayMs() {
        return retryDelayMs;
    }
}
