package com.shanthigear.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.shanthigear.repository.VendorRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VendorImportIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VendorRepository vendorRepository;

    @BeforeEach
    void setUp() {
        // Clear any existing test data
        vendorRepository.deleteAll();
    }


    @Test
    void importVendors_WithValidExcel_ShouldImportSuccessfully() throws Exception {
        // Create a test Excel file
        byte[] excelContent = createTestExcelFile();
        
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "vendors.xlsx", 
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            excelContent
        );

        // When/Then
        mockMvc.perform(multipart("/api/v1/vendors/import")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(2))
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.errorCount").value(0));

        // Verify the vendors were saved
        mockMvc.perform(get("/api/v1/vendors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].vendorId").value("VENDOR001"))
                .andExpect(jsonPath("$.content[1].vendorId").value("VENDOR002"));
    }

    @Test
    void importVendors_WithInvalidExcel_ShouldReturnErrors() throws Exception {
        // Create an invalid Excel file (empty)
        byte[] excelContent = new byte[0];
        
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "empty.xlsx", 
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            excelContent
        );

        // When/Then
        mockMvc.perform(multipart("/api/v1/vendors/import")
                .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Failed to parse Excel file"));
    }

    @Test
    void importVendors_WithMissingRequiredFields_ShouldReturnValidationErrors() throws Exception {
        // Create an Excel file with missing required fields
        byte[] excelContent = createTestExcelWithMissingRequiredFields();
        
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "invalid-vendors.xlsx", 
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            excelContent
        );

        // When/Then
        mockMvc.perform(multipart("/api/v1/vendors/import")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(2))
                .andExpect(jsonPath("$.successCount").value(0))
                .andExpect(jsonPath("$.errorCount").value(2));
    }

    @Test
    void downloadImportTemplate_ShouldReturnExcelFile() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/v1/vendors/import/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=vendor-import-template.xlsx"))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
    }

    private byte[] createTestExcelFile() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Vendors");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Vendor ID", "Name", "Email", "Phone", "Bank Account", "Bank Name", "IFSC Code", "PAN Number"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            
            // Create data rows
            Object[][] data = {
                {"VENDOR001", "Test Vendor 1", "vendor1@test.com", "9876543210", "12345678901234", "Test Bank", "TEST0012345", "AAAAA1111A"},
                {"VENDOR002", "Test Vendor 2", "vendor2@test.com", "9876543211", "12345678901235", "Test Bank", "TEST0012346", "BBBBB2222B"}
            };
            
            for (int i = 0; i < data.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int j = 0; j < data[i].length; j++) {
                    if (data[i][j] instanceof String) {
                        row.createCell(j).setCellValue((String) data[i][j]);
                    } else if (data[i][j] instanceof Number) {
                        row.createCell(j).setCellValue(((Number) data[i][j]).doubleValue());
                    }
                }
            }
            
            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
    
    private byte[] createTestExcelWithMissingRequiredFields() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Vendors");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Vendor ID", "Name", "Email"}; // Missing required fields
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            
            // Create data rows with missing required fields
            Object[][] data = {
                {"VENDOR001", "Test Vendor 1", "vendor1@test.com"},
                {"VENDOR002", "Test Vendor 2", "invalid-email"}
            };
            
            for (int i = 0; i < data.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int j = 0; j < data[i].length; j++) {
                    if (data[i][j] instanceof String) {
                        row.createCell(j).setCellValue((String) data[i][j]);
                    }
                }
            }
            
            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
