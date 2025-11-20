package com.shanthigear.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final List<String> EXPECTED_TAGS = Arrays.asList(
        "vendor-controller", "vendor-bulk-import-controller", "payment-controller",
        "auth-controller", "user-controller", "audit-log-controller"
    );

    private static final List<String> EXPECTED_PATHS = Arrays.asList(
        "/api/v1/vendors", "/api/v1/vendors/import", 
        "/api/v1/payments/process", "/api/v1/auth/login",
        "/api/v1/users/me", "/api/v1/audit-logs"
    );

    @Test
    void shouldReturnOpenApiDocumentation() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").isNotEmpty())
                .andExpect(jsonPath("$.info").isNotEmpty())
                .andExpect(jsonPath("$.paths").isNotEmpty())
                .andReturn();

        // Parse the response to verify structure
        String jsonResponse = result.getResponse().getContentAsString();
        JsonNode rootNode = objectMapper.readTree(jsonResponse);

        // Verify tags
        JsonNode tagsNode = rootNode.path("tags");
        List<String> actualTags = tagsNode.findValues("name").stream()
                .map(JsonNode::asText)
                .collect(Collectors.toList());
        
        for (String expectedTag : EXPECTED_TAGS) {
            assertTrue(actualTags.contains(expectedTag), 
                String.format("Expected tag '%s' not found in API documentation", expectedTag));
        }

        // Verify paths
        JsonNode pathsNode = rootNode.path("paths");
        for (String expectedPath : EXPECTED_PATHS) {
            assertTrue(pathsNode.has(expectedPath), 
                String.format("Expected path '%s' not found in API documentation", expectedPath));
        }
    }

    @Test
    void shouldContainVendorEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/vendors'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/vendors'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/vendors/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/vendors/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/vendors/{id}'].delete").exists());
    }

    @Test
    void shouldContainPaymentEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/payments/process'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/payments/batch'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/payments/status/{paymentId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/payments/export'].get").exists());
    }

    @Test
    void shouldContainSecuritySchemes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
    }

    @Test
    void shouldContainErrorResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.responses.BadRequest").exists())
                .andExpect(jsonPath("$.components.responses.Unauthorized").exists())
                .andExpect(jsonPath("$.components.responses.Forbidden").exists())
                .andExpect(jsonPath("$.components.responses.NotFound").exists())
                .andExpect(jsonPath("$.components.responses.InternalServerError").exists());
    }

    @Test
    void shouldContainRequestAndResponseExamples() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.examples").isMap())
                .andExpect(jsonPath("$.components.examples.VendorRequestExample").exists())
                .andExpect(jsonPath("$.components.examples.PaymentRequestExample").exists())
                .andExpect(jsonPath("$.components.examples.ErrorResponseExample").exists());
    }

    @Test
    void shouldContainServerInformation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers").isArray())
                .andExpect(jsonPath("$.servers[0].url").value("http://localhost:8080"))
                .andExpect(jsonPath("$.servers[0].description").value("Local server"));
    }

    @Test
    void shouldContactInfoSection() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Vendor Payment Notifier API"))
                .andExpect(jsonPath("$.info.description").isNotEmpty())
                .andExpect(jsonPath("$.info.version").exists())
                .andExpect(jsonPath("$.info.contact").isMap())
                .andExpect(jsonPath("$.info.contact.name").exists())
                .andExpect(jsonPath("$.info.contact.email").exists())
                .andExpect(jsonPath("$.info.contact.url").exists());
    }

    @Test
    void shouldContainSecurityRequirements() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.security").isArray())
                .andExpect(jsonPath("$.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/vendors'].get.security[0].bearerAuth").isArray());
    }
}
