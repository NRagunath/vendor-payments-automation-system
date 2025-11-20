package com.shanthigear.service;

import com.shanthigear.model.Vendor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorExcelReaderTest {

    private VendorExcelReader vendorExcelReader;
    
    @Mock
    private Row mockRow;
    
    @Mock
    private Cell mockCell;
    
    @BeforeEach
    void setUp() {
        vendorExcelReader = new VendorExcelReader();
    }
    
    @Test
    void readVendors_WithValidExcel_ReturnsVendors() throws IOException {
        // Create a simple Excel file in memory
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Vendors");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "Vendor Number", "Vendor Name", "Vendor Site", "Pay Group",
                "Bank Account Num", "Bank Name", "IFSC Code", "Branch",
                "Email Address", "Vendor Type", "Start Date Activity"
            };
            
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            
            // Create data row
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("VENDOR123");
            dataRow.createCell(1).setCellValue("Test Vendor");
            dataRow.createCell(2).setCellValue("SITE1");
            dataRow.createCell(3).setCellValue("PAYGROUP1");
            dataRow.createCell(4).setCellValue("1234567890");
            dataRow.createCell(5).setCellValue("Test Bank");
            dataRow.createCell(6).setCellValue("TEST0123456");
            dataRow.createCell(7).setCellValue("Test Branch");
            dataRow.createCell(8).setCellValue("test@example.com");
            dataRow.createCell(9).setCellValue("SUPPLIER");
            dataRow.createCell(10).setCellValue("2025-01-01");
            
            // Convert workbook to bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            // Create MultipartFile
            MultipartFile file = new MockMultipartFile(
                "test.xlsx",
                "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                outputStream.toByteArray()
            );
            
            // Test
            List<Vendor> vendors = vendorExcelReader.readVendors(file);
            
            // Verify
            assertNotNull(vendors);
            assertEquals(1, vendors.size());
            
            Vendor vendor = vendors.get(0);
            assertEquals("VENDOR123", vendor.getVendorNumber());
            assertEquals("Test Vendor", vendor.getVendorName());
            assertEquals("1234567890", vendor.getBankAccountNum());
            assertEquals("TEST0123456", vendor.getIfscCode());
        }
    }
    
    @Test
    void mapRowToVendor_WithValidData_ReturnsVendor() {
        // Setup mock row with cells
        when(mockRow.getCell(0)).thenReturn(createCell("VENDOR123"));
        when(mockRow.getCell(1)).thenReturn(createCell("Test Vendor"));
        when(mockRow.getCell(4)).thenReturn(createCell("1234567890"));
        when(mockRow.getCell(5)).thenReturn(createCell("Test Bank"));
        when(mockRow.getCell(6)).thenReturn(createCell("TEST0123456"));
        
        // Test
        Vendor vendor = vendorExcelReader.mapRowToVendor(mockRow);
        
        // Verify
        assertNotNull(vendor);
        assertEquals("VENDOR123", vendor.getVendorNumber());
        assertEquals("Test Vendor", vendor.getVendorName());
        assertEquals("1234567890", vendor.getBankAccountNum());
        assertEquals("TEST0123456", vendor.getIfscCode());
    }
    
    @Test
    void mapRowToVendor_WithMissingRequiredFields_ReturnsNull() {
        // Setup mock row with missing required fields
        when(mockRow.getCell(0)).thenReturn(createCell("")); // Empty vendor ID
        when(mockRow.getCell(1)).thenReturn(createCell("")); // Empty vendor name
        when(mockRow.getCell(4)).thenReturn(createCell("")); // Empty bank account
        when(mockRow.getCell(5)).thenReturn(createCell("")); // Empty bank name
        when(mockRow.getCell(6)).thenReturn(createCell("")); // Empty IFSC code
        
        // Test
        Vendor vendor = vendorExcelReader.mapRowToVendor(mockRow);
        
        // Verify
        assertNull(vendor);
    }
    
    private Cell createCell(String value) {
        Cell cell = mock(Cell.class);
        when(cell.getStringCellValue()).thenReturn(value);
        return cell;
    }
}
