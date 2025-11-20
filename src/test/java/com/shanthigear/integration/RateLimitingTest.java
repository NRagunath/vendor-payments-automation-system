package com.shanthigear.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ratelimit-test") // Use a different profile for rate limit testing
class RateLimitingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenRequestWithinRateLimit_thenOk() throws Exception {
        // First request should be allowed
        mockMvc.perform(get("/api/v1/rate-limit-test"))
                .andExpect(status().isOk());
    }

    @Test
    void whenRequestExceedsRateLimit_thenTooManyRequests() throws Exception {
        // Make multiple requests to exceed the rate limit
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/rate-limit-test"));
        }

        // Next request should be rate limited
        mockMvc.perform(get("/api/v1/rate-limit-test"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("X-Rate-Limit-Remaining"))
                .andExpect(header().exists("X-Rate-Limit-Retry-After-Seconds"));
    }

    @Test
    void whenDifferentEndpoints_thenSeparateRateLimits() throws Exception {
        // Make requests to different endpoints
        mockMvc.perform(get("/api/v1/rate-limit-test/1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/rate-limit-test/2"))
                .andExpect(status().isOk());
    }

    @Test
    void whenDifferentUsers_thenSeparateRateLimits() throws Exception {
        // User 1 makes requests
        mockMvc.perform(get("/api/v1/rate-limit-test")
                .header("X-Forwarded-For", "192.168.1.1"))
                .andExpect(status().isOk());

        // User 2 makes requests
        mockMvc.perform(get("/api/v1/rate-limit-test")
                .header("X-Forwarded-For", "192.168.1.2"))
                .andExpect(status().isOk());
    }
}
