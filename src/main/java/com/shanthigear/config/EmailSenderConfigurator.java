package com.shanthigear.config;

import com.shanthigear.entity.EmailDomainConfig;
import com.shanthigear.repository.EmailDomainConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.Optional;

/**
 * Configures and creates JavaMailSender instances based on domain configurations.
 * Supports both in-memory and database-stored configurations.
 */
@Component
public class EmailSenderConfigurator {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailSenderConfigurator.class);
    private final EmailConfig emailConfig;
    private final EmailDomainConfigRepository domainConfigRepository;
    
    public EmailSenderConfigurator(EmailConfig emailConfig, 
                                 EmailDomainConfigRepository domainConfigRepository) {
        this.emailConfig = emailConfig;
        this.domainConfigRepository = domainConfigRepository;
    }
    
    /**
     * Create a JavaMailSender instance for a specific domain.
     * @param domain The email domain to create a sender for
     * @return Configured JavaMailSender instance
     */
    public JavaMailSender createMailSender(String domain) {
        EmailConfig.DefaultConfig config = getConfigForDomain(domain);
        logger.debug("Creating mail sender for domain: {} with config: {}", domain, 
            config != null ? "[CONFIGURED]" : "[USING DEFAULT]");
        
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        configureMailSender(mailSender, config);
        
        return mailSender;
    }
    
    private EmailConfig.DefaultConfig getConfigForDomain(String domain) {
        // Check database first
        Optional<EmailDomainConfig> domainConfig = domainConfigRepository.findByDomain(domain);
        if (domainConfig.isPresent()) {
            return mapToConfig(domainConfig.get());
        }
        
        // Fall back to in-memory config
        logger.debug("No database config found for domain: {}, using in-memory config", domain);
        return emailConfig.getConfigForDomain(domain);
    }
    
    private EmailConfig.DefaultConfig mapToConfig(EmailDomainConfig domainConfig) {
        EmailConfig.DefaultConfig config = new EmailConfig.DefaultConfig();
        config.setHost(domainConfig.getHost());
        config.setPort(domainConfig.getPort());
        config.setUsername(domainConfig.getUsername());
        config.setPassword(domainConfig.getPassword());
        config.setProtocol(domainConfig.getProtocol());
        config.setAuth(domainConfig.isAuth());
        config.setStarttlsEnable(domainConfig.isStarttlsEnable());
        config.setSslTrust(domainConfig.isSslTrust());
        config.setConnectionTimeout(domainConfig.getConnectionTimeout());
        config.setTimeout(domainConfig.getTimeout());
        config.setWriteTimeout(domainConfig.getWriteTimeout());
        return config;
    }
    
    /**
     * Configure an existing JavaMailSender instance with the given configuration.
     * @param mailSender The mail sender to configure
     * @param config The configuration to apply
     */
    public void configureMailSender(JavaMailSenderImpl mailSender, EmailConfig.DefaultConfig config) {
        mailSender.setHost(config.getHost());
        mailSender.setPort(config.getPort());
        mailSender.setUsername(config.getUsername());
        mailSender.setPassword(config.getPassword());
        mailSender.setProtocol(config.getProtocol());
        mailSender.setDefaultEncoding(emailConfig.getTemplate().getEncoding());
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", config.getProtocol());
        props.put("mail.smtp.auth", String.valueOf(config.isAuth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(config.isStarttlsEnable()));
        props.put("mail.smtp.ssl.trust", config.getHost());
        props.put("mail.smtp.connectiontimeout", config.getConnectionTimeout());
        props.put("mail.smtp.timeout", config.getTimeout());
        props.put("mail.smtp.writetimeout", config.getWriteTimeout());
        props.put("mail.debug", "true");
    }
    
    /**
     * Extract the domain from an email address.
     * @param email The email address
     * @return The domain part of the email
     */
    public String extractDomain(String email) {
        if (email == null || email.isEmpty()) {
            return "";
        }
        int atIndex = email.lastIndexOf('@');
        if (atIndex == -1) {
            return "";
        }
        return email.substring(atIndex + 1).toLowerCase().trim();
    }
}
