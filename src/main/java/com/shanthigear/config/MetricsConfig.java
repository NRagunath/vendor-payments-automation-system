package com.shanthigear.config;

import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for application metrics.
 */
@Configuration
public class MetricsConfig {

    /**
     * Configures JVM and system metrics collection.
     */
    @Bean
    public MeterBinder systemMetrics() {
        // Create all metrics first
        ClassLoaderMetrics classLoaderMetrics = new ClassLoaderMetrics();
        JvmMemoryMetrics jvmMemoryMetrics = new JvmMemoryMetrics();
        // JvmGcMetrics is created and managed by Spring's lifecycle
        JvmGcMetrics jvmGcMetrics = new JvmGcMetrics();
        JvmThreadMetrics jvmThreadMetrics = new JvmThreadMetrics();
        UptimeMetrics uptimeMetrics = new UptimeMetrics();
        
        // Register shutdown hook to clean up resources
        Runtime.getRuntime().addShutdownHook(new Thread(jvmGcMetrics::close));
        
        return registry -> {
            // JVM metrics
            classLoaderMetrics.bindTo(registry);
            jvmMemoryMetrics.bindTo(registry);
            jvmGcMetrics.bindTo(registry);
            jvmThreadMetrics.bindTo(registry);
            
            // System metrics - ProcessorMetrics is already included by default in Spring Boot
            uptimeMetrics.bindTo(registry);
        };
    }
    
    /**
     * Configures a custom metrics endpoint.
     */
    @Bean
    public CustomMetricsEndpoint customMetricsEndpoint() {
        return new CustomMetricsEndpoint();
    }
    
    /**
     * Custom metrics endpoint for application-specific metrics.
     */
    public static class CustomMetricsEndpoint {
        // Add custom metrics methods here if needed
    }
}
