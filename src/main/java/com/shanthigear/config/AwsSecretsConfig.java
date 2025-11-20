package com.shanthigear.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@Configuration
@Profile({"prod", "staging"}) // Only enable in production and staging environments
public class AwsSecretsConfig {

    @Bean
    public SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
                .region(Region.AP_SOUTH_1) // Update with your AWS region
                .build();
    }

    @Bean
    @ConfigurationProperties(prefix = "aws.secrets")
    public AwsSecretsProperties awsSecretsProperties() {
        return new AwsSecretsProperties();
    }

    public static class AwsSecretsProperties {
        private String prefix = "/vendor-payment-notifier";

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
    }
}
