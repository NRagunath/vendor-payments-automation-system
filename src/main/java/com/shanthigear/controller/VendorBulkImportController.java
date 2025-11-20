package com.shanthigear.controller;

import com.shanthigear.dto.BulkImportResponse;
import com.shanthigear.service.VendorBulkImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/vendors")
@RequiredArgsConstructor
public class VendorBulkImportController {
    private final VendorBulkImportService importService;
    
    @PostMapping(value = "/bulk-import", consumes = "multipart/form-data")
    public ResponseEntity<BulkImportResponse> bulkImportVendors(
            @RequestParam("file") MultipartFile file) {
        
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a file to upload");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Only Excel (.xlsx) files are allowed");
        }
        
        try {
            BulkImportResponse response = importService.processBulkImport(file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process file: " + e.getMessage(), e);
        }
    }
    
    @GetMapping("/export-template")
    public ResponseEntity<byte[]> exportTemplate() {
        // Implementation to generate and return Excel template
        // Return 404 for now as template generation is not implemented
        return ResponseEntity.notFound().build();
    }
}
