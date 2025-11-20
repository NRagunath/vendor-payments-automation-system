package com.shanthigear.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import jakarta.servlet.MultipartConfigElement;

/**
 * Configuration for file upload settings.
 */
@Configuration
public class FileUploadConfig {
    
    /**
     * Configures multipart file upload settings.
     * 
     * @return MultipartConfigElement with configured settings
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        
        // Set max file size (10MB)
        factory.setMaxFileSize(DataSize.ofMegabytes(10));
        
        // Set max request size (20MB)
        factory.setMaxRequestSize(DataSize.ofMegabytes(20));
        
        // Set the location where files will be temporarily stored during upload
        // Default is the system temp directory
        // factory.setLocation("/tmp");
        
        return factory.createMultipartConfig();
    }
}
