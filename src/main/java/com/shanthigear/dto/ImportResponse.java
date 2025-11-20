package com.shanthigear.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for import operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for import operations")
public class ImportResponse {
    
    @Schema(description = "Indicates if the import was successful", example = "true")
    private boolean success;
    
    @Schema(description = "Number of records successfully imported", example = "5")
    private int importedCount;
    
    @Schema(description = "Number of records that failed to import", example = "1")
    private int errorCount;
    
    @Schema(description = "Summary message of the import operation", 
             example = "Import completed. Success: 5, Failed: 1")
    private String message;
    
    @Schema(description = "Import job ID for tracking status", example = "12345")
    private String importId;
    
    @Schema(description = "URL to check the status of the import", 
            example = "/api/v1/vendor-imports/12345/status")
    private String statusUrl;
    
    /**
     * Creates a success response for synchronous imports.
     */
    public static ImportResponse success(int importedCount, int errorCount) {
        return ImportResponse.builder()
                .success(true)
                .importedCount(importedCount)
                .errorCount(errorCount)
                .message(String.format("Import completed. Success: %d, Failed: %d", 
                    importedCount, errorCount))
                .build();
    }
    
    /**
     * Creates a success response for asynchronous imports.
     */
    public static ImportResponse accepted(String importId) {
        return ImportResponse.builder()
                .success(true)
                .message("Import started successfully")
                .importId(importId)
                .statusUrl("/api/v1/vendor-imports/" + importId + "/status")
                .build();
    }
    
    /**
     * Creates an error response.
     */
    public static ImportResponse error(String message) {
        return ImportResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
