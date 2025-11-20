package com.shanthigear.integration;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicEndpoints_ShouldBeAccessibleWithoutAuthentication() throws Exception {
        // Health check endpoint
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        // API docs
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());

        // Swagger UI
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }


    @Test
    @WithMockUser(roles = "USER")
    void userEndpoints_ShouldBeAccessibleWithUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/vendors"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/payments/status/123"))
                .andExpect(status().isNotFound()); // 404 because payment doesn't exist, but endpoint is accessible
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminEndpoints_ShouldBeAccessibleWithAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void adminEndpoints_ShouldBeForbiddenForNonAdminUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedEndpoints_ShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/vendors"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void csrf_ShouldBeEnabledForModifyingRequests() throws Exception {
        // GET requests should work without CSRF
        mockMvc.perform(get("/api/v1/vendors"))
                .andExpect(status().isOk());

        // POST/PUT/DELETE requests should fail without CSRF token
        mockMvc.perform(post("/api/v1/vendors")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden()); // CSRF token missing
    }

    @Test
    @WithMockUser(roles = "USER")
    void cors_ShouldBeConfiguredCorrectly() throws Exception {
        mockMvc.perform(options("/api/v1/vendors")
                .header("Access-Control-Request-Method", "GET")
                .header("Origin", "http://localhost:3000"))
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")));
    }

    @Test
    @WithMockUser(roles = "USER")
    void rateLimiting_ShouldBeApplied() throws Exception {
        // First request should succeed
        mockMvc.perform(get("/api/v1/rate-limit-test"))
                .andExpect(status().isOk());

        // Subsequent requests should be rate limited
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/rate-limit-test"));
        }

        mockMvc.perform(get("/api/v1/rate-limit-test"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("X-Rate-Limit-Remaining"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void sessionManagement_ShouldBeConfiguredCorrectly() throws Exception {
        mockMvc.perform(get("/api/v1/session-test"))
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().exists("X-XSS-Protection"));
    }

    @Test
    @WithMockUser(roles = "USER", username = "testuser")
    void authentication_ShouldSetPrincipalCorrectly() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void methodLevelSecurity_ShouldBeEnforced() throws Exception {
        // This test assumes there's a method with @PreAuthorize("hasRole('ADMIN')")
        mockMvc.perform(get("/api/v1/admin/method-secure"))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_ShouldReturnJwtToken() throws Exception {
        String credentials = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void refreshToken_ShouldReturnNewAccessToken() throws Exception {
        String refreshToken = "valid-refresh-token";
        
        mockMvc.perform(post("/api/v1/auth/refresh-token")
                .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }
}
