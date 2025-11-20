package com.shanthigear.controller;

import com.shanthigear.exception.BatchProcessingException;
import com.shanthigear.model.VendorPayment;
import com.shanthigear.service.VendorPaymentExcelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/vendor-payments/excel")
@RequiredArgsConstructor
public class VendorPaymentExcelController {

    private final VendorPaymentExcelService vendorPaymentExcelService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadExcelFile(
            @RequestParam("file") MultipartFile file) {
        
        log.info("Received Excel file upload: {}", file.getOriginalFilename());
        
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Please select a file to upload");
        }

        // Validate file type
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        boolean isExcelFile = contentType != null && 
            (contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
             contentType.equals("application/vnd.ms-excel"));
        boolean hasExcelExtension = originalFilename != null && 
            (originalFilename.endsWith(".xlsx") || originalFilename.endsWith(".xls"));
            
        if (!isExcelFile || !hasExcelExtension) {
            throw new IllegalArgumentException("Only Excel files (.xlsx or .xls) are allowed");
        }

        try {
            List<VendorPayment> processedPayments = vendorPaymentExcelService.processExcelFile(file);
            log.info("Processed {} payments from Excel file", processedPayments.size());
            return ResponseEntity.ok(processedPayments);
        } catch (BatchProcessingException e) {
            log.error("Error processing batch payments: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(
                Map.of("error", "Error processing batch payments",
                      "message", e.getMessage()));
        }
    }

    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate() {
        try {
            log.info("Generating Excel template for vendor payments");
            byte[] templateBytes = vendorPaymentExcelService.generateTemplate();
            ByteArrayResource resource = new ByteArrayResource(templateBytes);
            String filename = "vendor_payment_template.xlsx";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load template: " + e.getMessage(), e);
        }
    }
}
