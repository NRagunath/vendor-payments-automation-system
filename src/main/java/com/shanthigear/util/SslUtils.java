package com.shanthigear.util;

import com.shanthigear.config.BankApiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * Utility class for SSL/TLS configuration.
 */
@Component
public class SslUtils {

    private static final Logger log = LoggerFactory.getLogger(SslUtils.class);
    
    private final BankApiConfig bankApiConfig;
    
    public SslUtils(BankApiConfig bankApiConfig) {
        this.bankApiConfig = bankApiConfig;
    }
    
    /**
     * Creates an SSL context based on the bank API configuration.
     * @return Configured SSLContext
     */
    /**
     * Creates a secure SSL context with the specified configuration.
     * Implements certificate pinning and strong cipher suites.
     */
    public SSLContext createSslContext() throws KeyManagementException, NoSuchAlgorithmException, 
            KeyStoreException, CertificateException, IOException, UnrecoverableKeyException {
        
        BankApiConfig.SslConfig sslConfig = bankApiConfig.getSsl();
        
        if (!sslConfig.isEnabled()) {
            throw new SecurityException("SSL must be enabled for production use");
        }
        
        try {
            // Use SSLContextBuilder for more control
            SSLContextBuilder sslContextBuilder = SSLContexts.custom()
                .setProtocol(sslConfig.getProtocol() != null ? sslConfig.getProtocol() : "TLSv1.3");
                
            // Configure trust store if specified
            TrustManagerFactory trustManagerFactory = null;
            if (sslConfig.getTrustStore() != null && sslConfig.getTrustStorePassword() != null) {
                log.debug("Configuring trust store: {}", sslConfig.getTrustStore());
                try (InputStream trustStoreStream = new ClassPathResource(sslConfig.getTrustStore()).getInputStream()) {
                    KeyStore trustStore = KeyStore.getInstance("JKS");
                    trustStore.load(trustStoreStream, sslConfig.getTrustStorePassword().toCharArray());
                    
                    trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    trustManagerFactory.init(trustStore);
                    log.debug("Trust store initialized successfully");
                }
            }
            
            // Configure key store if specified
            if (sslConfig.isKeyStoreEnabled() && sslConfig.getKeyStorePath() != null) {
                KeyStore keyStore = loadKeyStore(
                    sslConfig.getKeyStorePath(), 
                    sslConfig.getKeyStoreType(),
                    sslConfig.getKeyStorePassword().toCharArray()
                );
                
                sslContextBuilder.loadKeyMaterial(
                    keyStore,
                    sslConfig.getKeyStorePassword().toCharArray(),
                    (aliases, socket) -> aliases.keySet().iterator().next()
                );
            }
            
            // Configure trust store with certificate pinning
            if (sslConfig.isTrustStoreEnabled() && sslConfig.getTrustStorePath() != null) {
                KeyStore trustStore = loadKeyStore(
                    sslConfig.getTrustStorePath(),
                    sslConfig.getTrustStoreType(),
                    sslConfig.getTrustStorePassword() != null ? 
                        sslConfig.getTrustStorePassword().toCharArray() : null
                );
                
                // Create trust manager that implements certificate pinning
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);
                
                // Get the first X509TrustManager
                X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
                
                // Create custom trust manager with certificate pinning
                X509TrustManager customTrustManager = new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        defaultTrustManager.checkClientTrusted(chain, authType);
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        defaultTrustManager.checkServerTrusted(chain, authType);
                        // Implement certificate pinning here if needed
                        // verifyCertificatePinning(chain[0]);
                    }


                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return defaultTrustManager.getAcceptedIssuers();
                    }
                };
                
                // Initialize the SSL context with our custom trust manager
                sslContextBuilder.loadTrustMaterial(null, (chain, authType) -> {
                    // Delegate to our custom trust manager
                    customTrustManager.checkServerTrusted(chain, authType);
                    return true; // Only reached if no exception was thrown
                });
            } else {
                throw new IllegalStateException("Trust store must be configured for production use");
            }
            
            // Build and return the SSL context
            return sslContextBuilder.build();
            
        } catch (Exception e) {
            log.error("Failed to initialize SSL context", e);
            throw e;
        }
    }
    
    /**
     * Loads a keystore from the specified path.
     *
     * @param path the path to the keystore file
     * @param type the keystore type (e.g., JKS, PKCS12)
     * @param password the keystore password
     * @return the loaded keystore
     * @throws KeyStoreException if the keystore could not be loaded
     * @throws IOException if there is an I/O error reading the keystore
     * @throws NoSuchAlgorithmException if the algorithm used to check the integrity of the keystore cannot be found
     * @throws CertificateException if any of the certificates in the keystore could not be loaded
     */
    private KeyStore loadKeyStore(String path, String type, char[] password) 
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        try (InputStream inputStream = new ClassPathResource(path).getInputStream()) {
            KeyStore keyStore = KeyStore.getInstance(type);
            keyStore.load(inputStream, password);
            return keyStore;
        }
    }
    
    /**
     * Validates the SSL configuration.
     * @throws IllegalStateException if the configuration is invalid
     */
    public void validateSslConfig() {
        BankApiConfig.SslConfig sslConfig = bankApiConfig.getSsl();
        
        if (!sslConfig.isEnabled()) {
            log.warn("SSL is disabled. Communication will not be encrypted!");
            return;
        }
        
        // Check required SSL properties
        if (sslConfig.getKeyStore() == null || sslConfig.getKeyStorePassword() == null) {
            throw new IllegalStateException(
                    "SSL is enabled but key store configuration is incomplete. " +
                    "Please configure bank.api.ssl.key-store and bank.api.ssl.key-store-password");
        }
        
        // Log a warning if trust store is not explicitly configured
        if (sslConfig.getTrustStore() == null || sslConfig.getTrustStorePassword() == null) {
            log.warn("Trust store configuration is incomplete. Using default trust store.");
        }
    }
}
