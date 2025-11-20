package com.shanthigear.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import org.springframework.lang.NonNull;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * Configuration for SSL/TLS settings for secure communication with the bank's API.
 * Supports both development (trust all) and production (custom truststore) modes.
 */
@Configuration
public class SslConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(SslConfig.class);
    
    @Value("${oracle.h2h.ssl.enabled:false}")
    private boolean sslEnabled;
    
    @Value("${oracle.h2h.ssl.trust-all:false}")
    private boolean trustAllCertificates;
    
    @Value("${oracle.h2h.ssl.trust-store:}")
    private String trustStorePath;
    
    @Value("${oracle.h2h.ssl.trust-store-password:}")
    private String trustStorePassword;

    /**
     * Configures a RestTemplate with SSL support based on application properties.
     * In production, uses the JVM's default truststore and keystore.
     * In development, can be configured to trust all certificates (not recommended for production).
     */
    @Bean
    @Qualifier("sslRestTemplate")
    public RestTemplate sslRestTemplate(RestTemplateBuilder builder) throws Exception {
        if (sslEnabled) {
            logger.info("Configuring SSL for bank API communication");
            
            // Create a custom SSL context
            SSLContext sslContext = createSslContext();
            
            // Create a custom request factory with our SSL context
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(@NonNull HttpURLConnection connection, @NonNull String httpMethod) throws IOException {
                    super.prepareConnection(connection, httpMethod);
                    if (connection instanceof HttpsURLConnection) {
                        HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                        httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                        httpsConnection.setHostnameVerifier((hostname, session) -> true);
                    }
                }
            };
            
            // Set timeouts
            requestFactory.setConnectTimeout(10000);
            requestFactory.setReadTimeout(10000);
            
            // Use the configured truststore
            System.setProperty("javax.net.ssl.trustStore", trustStorePath);
            System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
            
            return builder.requestFactory(() -> requestFactory).build();
        } else {
            logger.warn("SSL is disabled for bank API communication. This is not recommended for production!");
            return builder.build();
        }
    }

    /**
     * Creates an SSL context based on the application configuration.
     */
    private SSLContext createSslContext() throws NoSuchAlgorithmException, KeyManagementException {
        if (trustAllCertificates) {
            logger.warn("Using trust-all SSL context. This is not recommended for production!");
            return createTrustAllSslContext();
        } else {
            logger.info("Using default SSL context with JVM truststore");
            return SSLContext.getDefault();
        }
    }

    /**
     * Creates an SSL context that trusts all certificates (for development only).
     */
    private SSLContext createTrustAllSslContext() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());
        return sslContext;
    }
}
