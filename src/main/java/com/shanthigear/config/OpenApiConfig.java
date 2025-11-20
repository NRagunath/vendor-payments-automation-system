package com.shanthigear.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * Configuration for OpenAPI 3 documentation with enhanced security.
 * This configuration provides secure API documentation using SpringDoc OpenAPI 3.
 */
@Configuration
@Profile({"!prod"}) // Disable in production by default
public class OpenApiConfig {

    @Value("${springdoc.api-docs.enabled:true}")
    private boolean enableSwagger;

    @Value("${springdoc.swagger-ui.oauth2-redirect-url:}")
    private String oauth2RedirectUrl;

    @Bean
    public GroupedOpenApi vendorApi() {
        return GroupedOpenApi.builder()
                .group("vendor-apis")
                .pathsToMatch(
                    "/api/v1/vendors/**",
                    "/api/v1/payments/**"
                )
                .addOpenApiCustomizer(openApi -> 
                    openApi.info(apiInfo())
                )
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin-apis")
                .pathsToMatch("/api/v1/admin/**")
                .addOpenApiCustomizer(openApi -> 
                    openApi.info(apiInfo())
                )
                .build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        final String apiKeySchemeName = "apiKeyAuth";
        
        // Define the security schemes
        SecurityScheme bearerAuth = new SecurityScheme()
                .name(securitySchemeName)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT Authorization header using the Bearer scheme. Example: \"Authorization: Bearer {token}\"");

        SecurityScheme apiKeyAuth = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-KEY")
                .description("API Key for internal services");

        // Create components with security schemes
        Components components = new Components()
                .addSecuritySchemes(securitySchemeName, bearerAuth)
                .addSecuritySchemes(apiKeySchemeName, apiKeyAuth);

        return new OpenAPI()
                .components(components)
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .addSecurityItem(new SecurityRequirement().addList(apiKeySchemeName))
                .servers(List.of(
                    new Server()
                        .url("/")
                        .description("Default Server URL")
                ))
                .info(apiInfo())
                .externalDocs(new io.swagger.v3.oas.models.ExternalDocumentation()
                    .description("Vendor Payment Notifier Wiki Documentation")
                    .url("https://github.com/your-org/vendor-payment-notifier/wiki"));
    }

    private Info apiInfo() {
        return new Info()
                .title("Vendor Payment Notifier API")
                .description("""
                    <h2>Secure API Documentation</h2>
                    <p>This API documentation is restricted to authorized users only.</p>
                    <p>For access, please contact the system administrator.</p>
                    <h3>Authentication</h3>
                    <p>Use the <strong>Authorize</strong> button to authenticate using JWT token.</p>
                    """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Shanthi Gear Support")
                        .url("https://www.shanthigear.com/support")
                        .email("api-support@shanthigear.com"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://www.shanthigear.com/terms"))
                .termsOfService("https://www.shanthigear.com/terms");
    }
}
