package com.shanthigear.integration;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanthigear.model.Vendor;
import com.shanthigear.repository.VendorRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VendorManagementIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @SuppressWarnings("unused") // Used by Spring for JSON serialization/deserialization in test setup
    private ObjectMapper objectMapper;

    @Autowired
    private VendorRepository vendorRepository;

    private String testVendorId = "VENDOR-MGMT-001";

    @BeforeEach
    void setUp() {
        // Clear existing test data
        vendorRepository.deleteAll();

        // Create test vendors using builder pattern
        Vendor vendor1 = Vendor.builder()
            .vendorNumber(testVendorId)
            .vendorName("Test Vendor 1")
            .emailAddress("vendor1@test.com")
            .bankAccountNum("12345678901234")
            .bankName("Test Bank")
            .ifscCode("TEST0012345")
            .build();
        vendorRepository.save(vendor1);

        Vendor vendor2 = Vendor.builder()
            .vendorNumber("VENDOR-MGMT-002")
            .vendorName("Test Vendor 2")
            .emailAddress("vendor2@test.com")
            .bankAccountNum("12345678901235")
            .bankName("Test Bank")
            .ifscCode("TEST0012346")
            .build();
        vendorRepository.save(vendor2);
    }

    @Test
    void getAllVendors_ShouldReturnPaginatedResults() throws Exception {
        mockMvc.perform(get("/api/v1/vendors")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getVendorById_WithExistingId_ShouldReturnVendor() throws Exception {
        mockMvc.perform(get("/api/v1/vendors/{id}", testVendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendorId").value(testVendorId))
                .andExpect(jsonPath("$.name").value("Test Vendor 1"))
                .andExpect(jsonPath("$.email").value("vendor1@test.com"));
    }

    @Test
    void getVendorById_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/vendors/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Vendor not found with id: non-existent-id"));
    }

    @Test
    void createVendor_WithValidData_ShouldCreateVendor() throws Exception {
        String newVendorJson = """
        {
            "vendorId": "VENDOR-NEW-001",
            "name": "New Test Vendor",
            "email": "new.vendor@test.com",
            "phone": "9876543212",
            "bankAccount": "12345678901236",
            "bankName": "New Test Bank",
            "ifscCode": "TEST0012347",
            "panNumber": "CCCCC3333C"
        }
        """;

        mockMvc.perform(post("/api/v1/vendors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(newVendorJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vendorId").value("VENDOR-NEW-001"))
                .andExpect(jsonPath("$.name").value("New Test Vendor"))
                .andExpect(header().exists("Location"));
    }

    @Test
    void createVendor_WithDuplicateVendorId_ShouldReturnConflict() throws Exception {
        String duplicateVendorJson = String.format("""
        {
            "vendorId": "%s",
            "name": "Duplicate Vendor",
            "email": "duplicate@test.com"
        }
        """, testVendorId);

        mockMvc.perform(post("/api/v1/vendors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicateVendorJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Vendor already exists with id: " + testVendorId));
    }

    @Test
    void updateVendor_WithValidData_ShouldUpdateVendor() throws Exception {
        String updateVendorJson = """
        {
            "name": "Updated Vendor Name",
            "email": "updated.email@test.com",
            "phone": "9876543219"
        }
        """;

        mockMvc.perform(put("/api/v1/vendors/{id}", testVendorId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateVendorJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendorId").value(testVendorId))
                .andExpect(jsonPath("$.name").value("Updated Vendor Name"))
                .andExpect(jsonPath("$.email").value("updated.email@test.com"))
                .andExpect(jsonPath("$.phone").value("9876543219"));
    }

    @Test
    void deleteVendor_WithExistingId_ShouldDeleteVendor() throws Exception {
        mockMvc.perform(delete("/api/v1/vendors/{id}", testVendorId))
                .andExpect(status().isNoContent());

        // Verify vendor was deleted
        mockMvc.perform(get("/api/v1/vendors/{id}", testVendorId))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchVendors_WithQuery_ShouldReturnMatchingVendors() throws Exception {
        // Search by name
        mockMvc.perform(get("/api/v1/vendors/search")
                .param("query", "Vendor 1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Test Vendor 1"));

        // Search by email
        mockMvc.perform(get("/api/v1/vendors/search")
                .param("query", "vendor2@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email").value("vendor2@test.com"));
    }

    @Test
    void exportVendors_ShouldReturnCsvFile() throws Exception {
        mockMvc.perform(get("/api/v1/vendors/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("vendors-export-")))
                .andExpect(header().string("Content-Disposition", containsString(".csv")))
                .andExpect(content().contentType("text/csv"));
    }
}
