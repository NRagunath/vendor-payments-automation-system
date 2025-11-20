package com.shanthigear.controller;

import com.shanthigear.service.VendorImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for importing vendor data.
 */
@RestController
@RequestMapping("/api/v1/vendors/import")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Vendor Import", description = "APIs for importing vendor data")
public class VendorImportController {

    private final VendorImportService vendorImportService;

    /**
     * Import vendors from an Excel file.
     *
     * @param file The Excel file containing vendor data
     * @return Import result with success/failure information
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Import vendors from Excel",
        description = "Upload an Excel file containing vendor information to import into the system."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Vendors imported successfully",
        content = @Content(schema = @Schema(implementation = Map.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid file format or content",
        content = @Content(schema = @Schema(implementation = Map.class))
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error",
        content = @Content(schema = @Schema(implementation = Map.class))
    )
    public ResponseEntity<Map<String, Object>> importVendors(
            @RequestParam("file") MultipartFile file) {
        
        log.info("Received request to import vendors from file: {}", file.getOriginalFilename());
        
        try {
            VendorImportService.ImportResult result = vendorImportService.importVendors(file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("importedCount", result.importedCount());
            response.put("errorCount", result.errorCount());
            response.put("message", result.message());
            
            log.info("Vendor import completed: {}", result.message());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error importing vendors: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to import vendors: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Download the vendor import template.
     * 
     * @return The Excel template file
     */
    @GetMapping("/template")
    @Operation(
        summary = "Download vendor import template",
        description = "Download an Excel template for importing vendors."
    )
    public ResponseEntity<byte[]> downloadTemplate() {
        // This is a placeholder. In a real implementation, you would return an actual Excel file.
        // For now, returning a simple text file with instructions.
        String templateInstructions = "Vendor ID,Vendor Name,PAN Number,Email,Phone,Bank Account Number,Bank Name,Bank Branch,IFSC Code,Account Type\n" +
                "VENDOR001,Example Vendor,ABCDE1234F,example@vendor.com,9876543210,1234567890,Example Bank,Mumbai Branch,EXMP0123456,SAVINGS";
        
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=vendor_import_template.csv")
                .body(templateInstructions.getBytes());
    }
}
