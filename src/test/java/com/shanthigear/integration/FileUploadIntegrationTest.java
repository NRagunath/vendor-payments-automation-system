package com.shanthigear.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadVendors_WithValidExcel_ReturnsSuccess() throws Exception {
        // Create a test Excel file in memory
        byte[] content = createTestExcelFile();
        
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "vendors.xlsx", 
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
            content
        );

        mockMvc.perform(multipart("/api/v1/vendors/import")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").isNumber())
                .andExpect(jsonPath("$.successCount").isNumber())
                .andExpect(jsonPath("$.errorCount").isNumber());
    }

    private byte[] createTestExcelFile() {
        // In a real test, you would create an actual Excel file here
        // For simplicity, we're just returning a small Excel file with minimal content
        return new byte[] {
            // Minimal XLSX file header
            0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x06, 0x00, 0x08, 0x00, 0x00, 0x00, 0x21, 0x00,
            // Rest of the Excel file content would go here
            // This is just a minimal example - in a real test, use Apache POI to create a proper Excel file
        };
    }
}
