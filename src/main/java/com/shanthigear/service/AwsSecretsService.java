package com.shanthigear.service;

import com.shanthigear.config.AwsSecretsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AwsSecretsService {

    private final SecretsManagerClient secretsManagerClient;
    private final AwsSecretsConfig.AwsSecretsProperties awsSecretsProperties;

    @Cacheable(value = "secrets", key = "#secretName")
    public String getSecret(String secretName) {
        String secretKey = awsSecretsProperties.getPrefix() + "/" + secretName;
        
        try {
            log.info("Retrieving secret: {}", secretKey);
            GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
                    .secretId(secretKey)
                    .build();

            GetSecretValueResponse valueResponse = secretsManagerClient.getSecretValue(valueRequest);
            return valueResponse.secretString();
        } catch (SecretsManagerException e) {
            log.error("Error retrieving secret {}: {}", secretKey, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve secret: " + secretKey, e);
        }
    }
}
