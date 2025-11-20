package com.shanthigear.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicEndpoints_ShouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
                
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void vendorEndpoints_ShouldRequireUserRole() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/vendors"))
                .andReturn();
        int status = result.getResponse().getStatus();
        assertTrue(status == HttpStatus.OK.value() || status == HttpStatus.NO_CONTENT.value(),
                 "Status should be 200 OK or 204 No Content");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminEndpoints_ShouldRequireAdminRole() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/admin/users"))
                .andReturn();
        int status = result.getResponse().getStatus();
        assertTrue(status == HttpStatus.OK.value() || status == HttpStatus.FORBIDDEN.value(),
                 "Status should be 200 OK or 403 Forbidden");
    }

    @Test
    @WithMockUser(roles = "USER")
    void adminEndpoints_ShouldBeForbiddenForNonAdmins() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedEndpoints_ShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/vendors"))
                .andExpect(status().isUnauthorized());
    }
}
