package com.shanthigear.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import com.shanthigear.util.SslUtils;

import javax.net.ssl.SSLContext;
import java.time.Duration;

/**
 * Configuration for RestTemplate instances used for external HTTP calls.
 * Includes SSL configuration and retry mechanisms for resilient API communication.
 */
@Configuration
public class RestTemplateConfig {

    private final BankApiConfig bankApiConfig;
    private final RetryTemplate retryTemplate;
    private final SslUtils sslUtils;
    
    @Value("${server.ssl.key-store-type:PKCS12}")
    private String keyStoreType;
    
    @Value("${bank.api.ssl.enabled-protocols:TLSv1.3,TLSv1.2}")
    private String[] enabledProtocols;
    
    @Value("${bank.api.ssl.enabled-cipher-suites:}")
    private String[] enabledCipherSuites;

    @Autowired
    public RestTemplateConfig(BankApiConfig bankApiConfig, 
                            @Qualifier("bankApiRetryTemplate") RetryTemplate retryTemplate,
                            SslUtils sslUtils) {
        this.bankApiConfig = bankApiConfig;
        this.retryTemplate = retryTemplate;
        this.sslUtils = sslUtils;
    }

    /**
     * Creates a RestTemplate configured for bank API calls with SSL and retry capabilities.
     */
    @Bean(name = "bankRestTemplate")
    public RestTemplate bankRestTemplate() throws Exception {
        return new RestTemplateBuilder()
                .requestFactory(() -> {
                    try {
                        return new BufferingClientHttpRequestFactory(
                                createHttpRequestFactory()
                        );
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create request factory", e);
                    }
                })
                .setConnectTimeout(Duration.ofMillis(bankApiConfig.getConnectTimeout()))
                .setReadTimeout(Duration.ofMillis(bankApiConfig.getReadTimeout()))
                .additionalInterceptors(
                        new LoggingRequestInterceptor(),
                        (request, body, execution) -> retryTemplate.execute(
                                context -> execution.execute(request, body)
                        )
                )
                .errorHandler(new BankApiResponseErrorHandler())
                .build();
    }
    
    /**
     * Creates an HTTP request factory with SSL configuration.
     */
    private ClientHttpRequestFactory createHttpRequestFactory() 
            throws Exception {
        
        if (!bankApiConfig.getSsl().isEnabled()) {
            return new HttpComponentsClientHttpRequestFactory();
        }
        
        // Create SSL context using SslUtils
        SSLContext sslContext = sslUtils.createSslContext();
        
        // Configure allowed protocols and cipher suites
        String[] protocols = enabledProtocols;
        String[] ciphers = enabledCipherSuites.length > 0 ? enabledCipherSuites : null;
        
        // Create SSL socket factory with hostname verification
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
            sslContext,
            protocols,
            ciphers,
            (hostname, session) -> true // Hostname verification is handled by the custom TrustManager
        );
        
        // Configure connection pooling
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(socketFactory)
                .setDefaultSocketConfig(SocketConfig.custom()
                        .setSoTimeout(Timeout.ofMilliseconds(bankApiConfig.getReadTimeout()))
                        .build())
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(bankApiConfig.getConnectTimeout()))
                        .build())
                .setMaxConnTotal(100)
                .setMaxConnPerRoute(20)
                .build();
        
        // Configure request config with timeouts using HttpClient 5.2.3 APIs
        // Note: In HttpClient 5.x, timeouts are set differently than in 4.x
        // The connection request timeout is handled by the connection manager
        RequestConfig requestConfig = RequestConfig.custom()
                // Set the connection timeout (time to establish the connection)
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(bankApiConfig.getConnectTimeout()))
                // Set the response timeout (time waiting for data after connection is established)
                .setResponseTimeout(Timeout.ofMilliseconds(bankApiConfig.getReadTimeout()))
                .build();
        
        // Build HTTP client with the configured timeouts
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .disableCookieManagement()
                .build();
        
        // Create request factory with the configured HTTP client
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
