package com.shanthigear.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for payment service related beans and properties.
 */
@Configuration
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentServiceConfig {

    /**
     * Creates a RestTemplate bean for making HTTP requests to external services.
     * @return Configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    /**
     * Creates a PaymentProperties bean to access payment-related configuration.
     * @return PaymentProperties instance
     */
    @Bean
    public PaymentProperties paymentProperties() {
        return new PaymentProperties();
    }
}
