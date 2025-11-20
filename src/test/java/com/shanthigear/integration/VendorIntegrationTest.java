package com.shanthigear.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import com.shanthigear.model.Vendor;
import com.shanthigear.repository.VendorRepository;

@SqlGroup({
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:db/test-data.sql"),
    @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:db/cleanup.sql")
})
class VendorIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private VendorRepository vendorRepository;

    @Test
    void createVendor_WithValidData_ReturnsCreated() throws Exception {
        String vendorJson = """
        {
            "vendorId": "VENDOR001",
            "name": "Test Vendor",
            "email": "vendor@test.com",
            "phone": "1234567890",
            "bankAccount": "12345678901234",
            "bankName": "Test Bank",
            "ifscCode": "TEST0123456",
            "panNumber": "AAAAA1234A"
        }
        """;

        mockMvc.perform(post("/api/v1/vendors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(vendorJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vendorId").value("VENDOR001"))
                .andExpect(jsonPath("$.name").value("Test Vendor"));
    }

    @Test
    void getVendor_WithExistingId_ReturnsVendor() throws Exception {
        // Given
        Vendor vendor = Vendor.builder()
            .vendorNumber("VENDOR002")
            .vendorName("Existing Vendor")
            .emailAddress("existing@test.com")
            .build();
        vendorRepository.save(vendor);

        // When/Then
        mockMvc.perform(get("/api/v1/vendors/VENDOR002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendorId").value("VENDOR002"))
                .andExpect(jsonPath("$.name").value("Existing Vendor"));
    }

    @Test
    void updateVendor_WithValidData_ReturnsUpdatedVendor() throws Exception {
        // Given
        Vendor vendor = Vendor.builder()
            .vendorNumber("VENDOR003")
            .vendorName("Old Name")
            .emailAddress("old@test.com")
            .build();
        vendorRepository.save(vendor);

        String updateJson = """
        {
            "name": "Updated Name",
            "email": "updated@test.com"
        }
        """;

        // When/Then
        mockMvc.perform(put("/api/v1/vendors/VENDOR003")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("updated@test.com"));
    }

    @Test
    void deleteVendor_WithExistingId_ReturnsNoContent() throws Exception {
        // Given
        Vendor vendor = Vendor.builder()
            .vendorNumber("VENDOR004")
            .vendorName("To Be Deleted")
            .build();
        vendorRepository.save(vendor);

        // When/Then
        mockMvc.perform(delete("/api/v1/vendors/VENDOR004"))
                .andExpect(status().isNoContent());
    }

    @Test
    void importVendors_WithValidExcel_ReturnsSuccess() throws Exception {
        // This test would require a mock Excel file
        // For now, we'll test the endpoint structure
        mockMvc.perform(multipart("/api/v1/vendors/import")
                .file("file", "test.xlsx".getBytes()))
                .andExpect(status().isOk());
    }
}
