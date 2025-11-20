package com.shanthigear.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for email settings with support for multiple domains.
 */
@Configuration
@ConfigurationProperties(prefix = "app.email")
@Validated
public class EmailConfig {
    
    /**
     * Default email configuration
     */
    private final DefaultConfig defaultConfig = new DefaultConfig();
    
    /**
     * Domain-specific email configurations
     */
    private final Map<String, DomainConfig> domains = new HashMap<>();
    
    /**
     * Template configurations
     */
    private final TemplateConfig template = new TemplateConfig();
    
    // Getters and Setters
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }
    
    public Map<String, DomainConfig> getDomains() {
        return domains;
    }
    
    public TemplateConfig getTemplate() {
        return template;
    }
    
    /**
     * Get configuration for a specific domain
     * @param domain The domain to get configuration for
     * @return The domain configuration or default if not found
     */
    public DefaultConfig getConfigForDomain(String domain) {
        if (domain == null || domain.isEmpty()) {
            return defaultConfig;
        }
        // Safe to cast since DomainConfig extends DefaultConfig
        return domains.getOrDefault(domain.toLowerCase(), (DomainConfig) defaultConfig);
    }
    
    /**
     * Default email configuration
     */
    public static class DefaultConfig {
        @NotBlank
        private String host = "smtp.gmail.com";
        private int port = 587;
        private String username;
        private String password;
        private String protocol = "smtp";
        private String from;
        private String fromName;
        private boolean auth = true;
        private boolean starttlsEnable = true;
        private boolean sslTrust = true;
        private int connectionTimeout = 5000;
        private int timeout = 5000;
        private int writeTimeout = 5000;
        
        // Getters and Setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getProtocol() { return protocol; }
        public void setProtocol(String protocol) { this.protocol = protocol; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getFromName() { return fromName; }
        public void setFromName(String fromName) { this.fromName = fromName; }
        public boolean isAuth() { return auth; }
        public void setAuth(boolean auth) { this.auth = auth; }
        public boolean isStarttlsEnable() { return starttlsEnable; }
        public void setStarttlsEnable(boolean starttlsEnable) { this.starttlsEnable = starttlsEnable; }
        public boolean isSslTrust() { return sslTrust; }
        public void setSslTrust(boolean sslTrust) { this.sslTrust = sslTrust; }
        public int getConnectionTimeout() { return connectionTimeout; }
        public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
        public int getWriteTimeout() { return writeTimeout; }
        public void setWriteTimeout(int writeTimeout) { this.writeTimeout = writeTimeout; }
    }
    
    /**
     * Domain-specific email configuration
     */
    public static class DomainConfig extends DefaultConfig {
        // Can be extended with domain-specific settings
    }
    
    /**
     * Email template configuration
     */
    public static class TemplateConfig {
        private String prefix = "classpath:/templates/emails/";
        private String suffix = ".html";
        private String encoding = "UTF-8";
        private boolean cache = false;
        
        // Getters and Setters
        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
        public String getSuffix() { return suffix; }
        public void setSuffix(String suffix) { this.suffix = suffix; }
        public String getEncoding() { return encoding; }
        public void setEncoding(String encoding) { this.encoding = encoding; }
        public boolean isCache() { return cache; }
        public void setCache(boolean cache) { this.cache = cache; }
    }
}
