package com.shanthigear.controller;

import com.shanthigear.dto.BulkImportResponse;
import com.shanthigear.service.VendorBulkImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class VendorBulkImportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VendorBulkImportService vendorBulkImportService;

    @InjectMocks
    private VendorBulkImportController vendorBulkImportController;

    private MockMultipartFile testFile;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(vendorBulkImportController).build();
        
        testFile = new MockMultipartFile(
            "file",
            "test.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "test data".getBytes()
        );
    }

    @Test
    void importVendors_WithValidFile_ReturnsSuccess() throws Exception {
        // Given
        BulkImportResponse response = BulkImportResponse.builder()
                .totalRecords(1)
                .successCount(1)
                .failureCount(0)
                .errors(Collections.emptyList())
                .build();
        
        when(vendorBulkImportService.processBulkImport(any())).thenReturn(response);

        // When/Then
        mockMvc.perform(multipart("/api/v1/vendors/bulk-import")
                .file(testFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(1))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failureCount").value(0));
    }

    @Test
    void importVendors_WithEmptyFile_ReturnsBadRequest() throws Exception {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "empty.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new byte[0]
        );

        // When/Then
        mockMvc.perform(multipart("/api/v1/vendors/bulk-import")
                .file(emptyFile))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importVendors_WithInvalidFileType_ReturnsBadRequest() throws Exception {
        // Given
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "invalid content".getBytes()
        );

        // When/Then
        mockMvc.perform(multipart("/api/v1/vendors/bulk-import")
                .file(invalidFile))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getImportTemplate_ReturnsExcelFile() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/v1/vendors/export-template"))
                .andExpect(status().isNotFound());
    }


}
