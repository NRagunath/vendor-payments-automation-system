package com.shanthigear.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Configuration for retry mechanism when calling external services.
 */
@Configuration
@EnableRetry
public class RetryConfig {

    private final BankApiConfig bankApiConfig;

    public RetryConfig(BankApiConfig bankApiConfig) {
        this.bankApiConfig = bankApiConfig;
    }

    /**
     * Creates a RetryTemplate with exponential backoff for bank API calls.
     * @return Configured RetryTemplate
     */
    @Bean(name = "bankApiRetryTemplate")
    public RetryTemplate bankApiRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Configure retry policy
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(bankApiConfig.getRetryMaxAttempts());
        retryTemplate.setRetryPolicy(retryPolicy);

        // Configure exponential backoff policy
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(bankApiConfig.getRetryInitialInterval());
        backOffPolicy.setMultiplier(bankApiConfig.getRetryMultiplier());
        backOffPolicy.setMaxInterval(bankApiConfig.getRetryMaxInterval());
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }
}
