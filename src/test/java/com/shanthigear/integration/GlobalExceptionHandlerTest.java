package com.shanthigear.integration;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.shanthigear.VendorPaymentApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = VendorPaymentApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void handleResourceNotFoundException_ShouldReturnNotFoundResponse() throws Exception {
        // Try to get a non-existent vendor
        mockMvc.perform(get("/api/v1/vendors/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/v1/vendors/non-existent-id"));
    }

    @Test
    void handleMethodArgumentNotValid_ShouldReturnBadRequest() throws Exception {
        // Try to create a vendor with invalid data
        String invalidVendorJson = "{\"vendorId\":\"\", \"email\":\"invalid-email\"}";
        
        mockMvc.perform(post("/api/v1/vendors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidVendorJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors", hasSize(greaterThan(0))));
    }

    @Test
    void handleHttpMessageNotReadable_ShouldReturnBadRequest() throws Exception {
        // Send invalid JSON
        mockMvc.perform(post("/api/v1/vendors")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"invalid\":\"json\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void handleMethodNotSupportedException_ShouldReturnMethodNotAllowed() throws Exception {
        // Try to use an unsupported HTTP method
        mockMvc.perform(patch("/api/v1/vendors"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));
    }

    @Test
    void handleHttpMediaTypeNotSupported_ShouldReturnUnsupportedMediaType() throws Exception {
        // Send unsupported media type
        mockMvc.perform(post("/api/v1/vendors")
                .contentType(MediaType.APPLICATION_XML)
                .content("<vendor><name>Test</name></vendor>"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"));
    }
}
