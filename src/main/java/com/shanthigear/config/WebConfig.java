package com.shanthigear.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.annotation.*;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Web configuration class for the application.
 * Configures CORS, content negotiation, and other web-related settings.
 */
/**
 * Web configuration class for the application.
 * Configures CORS, content negotiation, and other web-related settings.
 */
@Configuration
public class WebConfig extends WebMvcConfigurationSupport {

    private static final long MAX_AGE_SECS = 3600;

    /**
     * Configures CORS mappings for the application.
     */
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(MAX_AGE_SECS);
    }

    /**
     * Configures content negotiation for the application.
     */
    @Override
    public void configureContentNegotiation(@NonNull ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON)
                 .favorParameter(false)
                 .ignoreAcceptHeader(false);
    }

    /**
     * Adds resource handlers for static resources.
     */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * Configures a request logging filter for development purposes.
     * Can be enabled/disabled via application properties.
     */
    // API Versioning Configuration
    @Bean
    @Override
    public RequestMappingHandlerMapping requestMappingHandlerMapping(
            ContentNegotiationManager contentNegotiationManager,
            FormattingConversionService conversionService,
            ResourceUrlProvider resourceUrlProvider) {
        
        ApiVersionRequestMappingHandlerMapping mapping = new ApiVersionRequestMappingHandlerMapping();
        mapping.setOrder(0);
        mapping.setInterceptors(getInterceptors(conversionService, resourceUrlProvider));
        mapping.setContentNegotiationManager(contentNegotiationManager);
        
        return mapping;
    }

    /**
     * Custom RequestMappingHandlerMapping to support API versioning.
     * Allows versioning via URL path.
     */
    public static class ApiVersionRequestMappingHandlerMapping extends RequestMappingHandlerMapping {
        private final Map<HandlerMethod, RequestMappingInfo> methodMap = new ConcurrentHashMap<>();
        private final Map<RequestMappingInfo, HandlerMethod> handlerMethods = new ConcurrentHashMap<>();

        @Override
        protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
            HandlerMethod handlerMethod = createHandlerMethod(handler, method);
            methodMap.put(handlerMethod, mapping);
            handlerMethods.put(mapping, handlerMethod);
            super.registerHandlerMethod(handler, method, mapping);
        }

        @Override
        protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
            RequestMappingInfo info = createRequestMappingInfo(method);
            if (info != null) {
                RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);
                if (typeInfo != null) {
                    info = typeInfo.combine(info);
                }
                
                // Add API version info if present
                ApiVersion version = MergedAnnotations.from(method).get(ApiVersion.class).synthesize();
                if (version == null) {
                    version = MergedAnnotations.from(handlerType).get(ApiVersion.class).synthesize();
                }
                
                if (version != null) {
                    info = RequestMappingInfo.paths(version.value())
                            .build()
                            .combine(info);
                }
            }
            return info;
        }

        private RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {
            MergedAnnotation<org.springframework.web.bind.annotation.RequestMapping> mappingAnnotation = 
                MergedAnnotations.from(element).get(org.springframework.web.bind.annotation.RequestMapping.class);
            return mappingAnnotation.isPresent() ? createRequestMappingFromAnnotation(mappingAnnotation) : null;
        }
        
        private RequestMappingInfo createRequestMappingFromAnnotation(
                MergedAnnotation<org.springframework.web.bind.annotation.RequestMapping> mapping) {
            return RequestMappingInfo
                .paths(mapping.getStringArray("path"))
                .methods(mapping.getStringArray("method").length > 0 ? 
                    mapping.getEnumArray("method", org.springframework.web.bind.annotation.RequestMethod.class) : null)
                .params(mapping.getStringArray("params"))
                .headers(mapping.getStringArray("headers"))
                .consumes(mapping.getStringArray("consumes"))
                .produces(mapping.getStringArray("produces"))
                .mappingName((String) mapping.getValue("name", String.class).orElse(null))
                .build();
        }
    }

    /**
     * API Version annotation for versioning RESTful APIs.
     * Can be applied to controller classes or handler methods.
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @RequestMapping
    public @interface ApiVersion {
        /**
         * The version number as part of the URL path.
         * Example: "v1" would map to "/v1/..."
         */
        String value();
    }

    /**
     * Request logging filter for development and debugging purposes.
     * Logs incoming requests with details like headers, parameters, and payload.
     */
    @Bean
    public RequestLoggingFilter requestLoggingFilter() {
        return createRequestLoggingFilter();
    }
    
    private static RequestLoggingFilter createRequestLoggingFilter() {
        RequestLoggingFilter filter = new RequestLoggingFilter();
        filter.setIncludeClientInfo(true);
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(64000);
        filter.setIncludeHeaders(true);
        filter.setAfterMessagePrefix("REQUEST DATA: ");
        return filter;
    }

    /**
     * Simple request logging filter for development purposes.
     */
    public static class RequestLoggingFilter extends org.springframework.web.filter.CommonsRequestLoggingFilter {
        @Override
        protected boolean shouldLog(jakarta.servlet.http.HttpServletRequest request) {
            // Only log requests for non-static resources and API endpoints
            String path = request.getRequestURI();
            return !path.startsWith("/static/") && 
                   !path.startsWith("/webjars/") && 
                   !path.startsWith("/swagger") &&
                   !path.startsWith("/v3/api-docs");
        }
    }
}
