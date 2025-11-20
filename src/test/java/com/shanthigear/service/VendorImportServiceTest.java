package com.shanthigear.service;

import com.shanthigear.model.Vendor;
import com.shanthigear.repository.VendorRepository;
import com.shanthigear.util.VendorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorImportServiceTest {

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private VendorExcelReader vendorExcelReader;

    @Mock
    private VendorUtils vendorUtils;

    @InjectMocks
    private VendorImportService vendorImportService;

    private MultipartFile testFile;
    private Vendor testVendor;

    @BeforeEach
    void setUp() {
        testFile = new MockMultipartFile(
            "test.xlsx",
            "test.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "test data".getBytes(StandardCharsets.UTF_8)
        );

        testVendor = Vendor.builder()
            .vendorNumber("VENDOR123")
            .vendorName("Test Vendor")
            .bankAccountNum("1234567890")
            .bankName("Test Bank")
            .ifscCode("TEST0123456")
            .build();
    }

    @Test
    void importVendors_WithValidFile_ReturnsSuccess() throws IOException {
        when(vendorExcelReader.readVendors(any(MultipartFile.class)))
            .thenReturn(List.of(testVendor));
        when(vendorRepository.findByVendorNumber(anyString()))
            .thenReturn(Optional.empty());
        when(vendorRepository.save(any(Vendor.class)))
            .thenReturn(testVendor);

        var result = vendorImportService.importVendors(testFile);

        assertNotNull(result);
        assertEquals(1, result.importedCount());
        assertEquals(0, result.errorCount());
        verify(vendorRepository, times(1)).save(any(Vendor.class));
    }

    @Test
    void importVendors_WithExistingVendor_UpdatesVendor() throws IOException {
        when(vendorExcelReader.readVendors(any(MultipartFile.class)))
            .thenReturn(List.of(testVendor));
        when(vendorRepository.findByVendorNumber(anyString()))
            .thenReturn(Optional.of(testVendor));
        when(vendorRepository.save(any(Vendor.class)))
            .thenReturn(testVendor);

        var result = vendorImportService.importVendors(testFile);

        assertNotNull(result);
        assertEquals(1, result.importedCount());
        assertEquals(0, result.errorCount());
        verify(vendorRepository, times(1)).save(any(Vendor.class));
    }

    @Test
    void importVendors_WithInvalidFile_ReturnsError() throws IOException {
        when(vendorExcelReader.readVendors(any(MultipartFile.class)))
            .thenThrow(new IOException("Invalid file format"));

        assertThrows(IOException.class, () -> vendorImportService.importVendors(testFile));
    }

    @Test
    void importVendors_WithValidationError_SkipsInvalidVendor() throws IOException {
        testVendor = Vendor.builder()
            .vendorNumber("") // Invalid vendor ID
            .vendorName("Test Vendor")
            .bankAccountNum("1234567890")
            .bankName("Test Bank")
            .ifscCode("TEST0123456")
            .build();
        when(vendorExcelReader.readVendors(any(MultipartFile.class)))
            .thenReturn(List.of(testVendor));

        var result = vendorImportService.importVendors(testFile);

        assertNotNull(result);
        assertEquals(0, result.importedCount());
        assertEquals(1, result.errorCount());
        verify(vendorRepository, never()).save(any(Vendor.class));
    }
}
