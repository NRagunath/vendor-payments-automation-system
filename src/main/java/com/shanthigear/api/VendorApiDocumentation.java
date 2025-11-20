package com.shanthigear.api;

import com.shanthigear.dto.ImportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * API documentation for Vendor Import operations.
 * This interface serves as a contract for the OpenAPI/Swagger documentation.
 */
@Tag(name = "Vendor Import", description = "APIs for importing vendor data from Excel files")
public interface VendorApiDocumentation {

    @Operation(
        summary = "Import vendors from Excel",
        description = "Upload an Excel file containing vendor data to import into the system.",
        requestBody = @RequestBody(
            description = "Excel file containing vendor data",
            required = true,
            content = @Content(
                mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                schema = @Schema(type = "string", format = "binary")
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Vendors imported successfully",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ImportResponse.class)
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid file format or data"
            ),
            @ApiResponse(
                responseCode = "413",
                description = "File size exceeds the maximum allowed limit (10MB)"
            )
        }
    )
    ResponseEntity<ImportResponse> importVendors(
        @Parameter(description = "Excel file containing vendor data") MultipartFile file
    );

    @Operation(
        summary = "Download import template",
        description = "Download an Excel template for importing vendors.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Template file downloaded successfully",
                content = @Content(
                    mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    schema = @Schema(type = "string", format = "binary")
                )
            )
        }
    )
    ResponseEntity<Resource> downloadTemplate();
}
