package com.shanthigear.controller;

import com.shanthigear.dto.VendorRequestDTO;
import com.shanthigear.dto.VendorResponseDTO;
import com.shanthigear.exception.InvalidVendorDataException;
import com.shanthigear.exception.ResourceNotFoundException;
import com.shanthigear.exception.VendorAlreadyExistsException;
import com.shanthigear.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for managing vendors.
 */
@Slf4j
@RestController
@RequestMapping("/api/vendors")
@Tag(
    name = "Vendors", 
    description = "APIs for managing vendor information",
    externalDocs = @io.swagger.v3.oas.annotations.ExternalDocumentation(
        description = "Vendor Management API Documentation",
        url = "https://api.shanthigear.com/docs/vendor-api"
    )
)
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create a new vendor",
        description = "Creates a new vendor with the provided details. Vendor number must be unique. Requires VENDOR_WRITE permission.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Vendor details to create",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VendorRequestDTO.class),
                examples = @ExampleObject(
                    name = "Create Vendor Example",
                    summary = "Create a new vendor with minimal required fields",
                    value = """
                    {
                      "vendorNumber": "VEND-001",
                      "vendorName": "ABC Suppliers",
                      "emailAddress": "contact@abcsuppliers.com",
                      "phoneNumber": "+911234567890",
                      "addressLine1": "123 Business Park",
                      "city": "Bangalore",
                      "state": "Karnataka",
                      "country": "India",
                      "pincode": "560001"
                    }
                    """
                )
            )
        )
    )
    @ApiResponse(
        responseCode = "201",
        description = "Vendor created successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = VendorResponseDTO.class)
        )
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid vendor data provided",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = Map.class),
            examples = @ExampleObject(
                value = """
                {
                  "timestamp": "2023-05-15T10:30:00Z",
                  "status": 400,
                  "error": "Bad Request",
                  "message": "Invalid vendor data",
                  "details": ["Vendor name is required", "Invalid email format"]
                }
                """
            )
        )
    )
    @ApiResponse(
        responseCode = "409",
        description = "Vendor with the same vendor number already exists",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = Map.class),
            examples = @ExampleObject(
                value = """
                {
                  "timestamp": "2023-05-15T10:30:00Z",
                  "status": 409,
                  "error": "Conflict",
                  "message": "Vendor with number VEND-001 already exists"
                }
                """
            )
        )
    )
    @PreAuthorize("hasAuthority('VENDOR_WRITE')")
    public ResponseEntity<VendorResponseDTO> createVendor(
            @Valid @RequestBody VendorRequestDTO vendorRequestDTO) 
            throws VendorAlreadyExistsException, InvalidVendorDataException {
        
        log.info("Received request to create vendor: {}", vendorRequestDTO.getVendorNumber());
        VendorResponseDTO createdVendor = vendorService.createVendor(vendorRequestDTO);
        log.info("Successfully created vendor: {}", createdVendor.getVendorNumber());
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("X-Vendor-Number", createdVendor.getVendorNumber())
                .body(createdVendor);
    }

    @PutMapping(value = "/{vendorNumber}", 
                consumes = MediaType.APPLICATION_JSON_VALUE, 
                produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Update a vendor",
        description = "Updates an existing vendor identified by vendor number. All fields will be updated with the provided values. Requires VENDOR_WRITE permission."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Vendor updated successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = VendorResponseDTO.class)
        )
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid vendor data provided",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = Map.class)
        )
    )
    @ApiResponse(
        responseCode = "404",
        description = "Vendor not found",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = Map.class),
            examples = @ExampleObject(
                value = """
                {
                  "timestamp": "2023-05-15T10:30:00Z",
                  "status": 404,
                  "error": "Not Found",
                  "message": "Vendor not found with number: VEND-999"
                }
                """
            )
        )
    )
    @PreAuthorize("hasAuthority('VENDOR_WRITE')")
    public ResponseEntity<VendorResponseDTO> updateVendor(
            @Parameter(
                description = "Vendor number of the vendor to update",
                example = "VEND-001",
                required = true
            ) 
            @PathVariable String vendorNumber,
            @Valid @RequestBody VendorRequestDTO vendorRequestDTO)
            throws ResourceNotFoundException, InvalidVendorDataException {
        
        log.info("Received request to update vendor: {}", vendorNumber);
        VendorResponseDTO updatedVendor = vendorService.updateVendor(vendorNumber, vendorRequestDTO);
        log.info("Successfully updated vendor: {}", vendorNumber);
        
        return ResponseEntity.ok(updatedVendor);
    }

    @GetMapping(value = "/{vendorNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get a vendor by vendor number",
        description = "Retrieves detailed information about a specific vendor using their unique vendor number. Requires VENDOR_READ permission.",
        parameters = {
            @Parameter(
                name = "vendorNumber",
                description = "Unique identifier of the vendor",
                example = "VEND-001",
                required = true
            )
        }
    )
    @ApiResponse(
        responseCode = "200",
        description = "Vendor found",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = VendorResponseDTO.class),
            examples = @ExampleObject(
                name = "Vendor Details",
                summary = "Example of a vendor response",
                value = """
                {
                  "vendorNumber": "VEND-001",
                  "vendorName": "ABC Suppliers",
                  "emailAddress": "contact@abcsuppliers.com",
                  "phoneNumber": "+911234567890",
                  "addressLine1": "123 Business Park",
                  "addressLine2": "Electronic City",
                  "city": "Bangalore",
                  "state": "Karnataka",
                  "country": "India",
                  "pincode": "560001",
                  "gstNumber": "29AABCS1234M1Z5",
                  "panNumber": "AABCS1234M",
                  "bankAccountNum": "1234567890",
                  "bankName": "HDFC Bank",
                  "ifscCode": "HDFC0001234",
                  "branchName": "Electronic City Branch",
                  "isActive": true,
                  "createdAt": "2023-01-15T10:30:00Z",
                  "updatedAt": "2023-05-10T15:45:30Z"
                }
                """
            )
        )
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid vendor number format",
        content = @Content(schema = @Schema(hidden = true))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Vendor not found",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = Map.class)
        )
    )
    @PreAuthorize("hasAuthority('VENDOR_READ')")
    public ResponseEntity<VendorResponseDTO> getVendorByVendorNumber(
            @PathVariable String vendorNumber) 
            throws ResourceNotFoundException {
        
        log.debug("Fetching vendor with number: {}", vendorNumber);
        VendorResponseDTO vendor = vendorService.getVendorByVendorNumber(vendorNumber);
        
        return ResponseEntity.ok()
                .eTag(String.valueOf(vendor.hashCode()))
                .body(vendor);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get all vendors",
        description = "Retrieves a paginated and sorted list of all vendors. Supports filtering and sorting. Requires VENDOR_READ permission.",
        parameters = {
            @Parameter(
                name = "page",
                description = "Page number (0-based)",
                example = "0"
            ),
            @Parameter(
                name = "size",
                description = "Number of items per page",
                example = "20"
            ),
            @Parameter(
                name = "sort",
                description = "Sorting criteria in the format: property,asc|desc. Default sort is by vendorName,asc",
                example = "vendorName,asc"
            )
        }
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of vendors retrieved successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = Page.class)
        )
    )
    @PreAuthorize("hasAuthority('VENDOR_READ')")
    public ResponseEntity<Page<VendorResponseDTO>> getAllVendors(
            @ParameterObject @PageableDefault(
                size = 20, 
                sort = "vendorName", 
                direction = org.springframework.data.domain.Sort.Direction.ASC
            ) Pageable pageable) {
        
        log.debug("Fetching all vendors with page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<VendorResponseDTO> vendors = vendorService.getAllVendors(pageable);
        
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(vendors.getTotalElements()))
                .body(vendors);
    }

    @GetMapping(path = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Search vendors",
        description = "Searches vendors by name, email, or other attributes using a free-text search query. Requires VENDOR_READ permission.",
        parameters = {
            @Parameter(
                name = "query",
                description = "Search query to match against vendor name, email, or other attributes",
                required = true,
                example = "ABC Suppliers"
            ),
            @Parameter(
                name = "page",
                description = "Page number (0-based)",
                example = "0"
            ),
            @Parameter(
                name = "size",
                description = "Number of items per page",
                example = "20"
            )
        }
    )
    @ApiResponse(
        responseCode = "200",
        description = "Search results matching the query",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = Page.class)
        )
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid search query",
        content = @Content(schema = @Schema(hidden = true))
    )
    @PreAuthorize("hasAuthority('VENDOR_READ')")
    public ResponseEntity<Page<VendorResponseDTO>> searchVendors(
            @RequestParam(required = false) String query,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        
        if (query == null || query.trim().length() < 2) {
            throw new InvalidVendorDataException("Search query must be at least 2 characters long");
        }
        
        log.debug("Searching vendors with query: '{}', page: {}, size: {}", 
                 query, pageable.getPageNumber(), pageable.getPageSize());
        Page<VendorResponseDTO> results = vendorService.searchVendors(query, pageable);
        
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(results.getTotalElements()))
                .body(results);
    }

    @DeleteMapping("/{vendorNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Delete a vendor",
        description = "Soft deletes the vendor with the specified vendor number by marking them as inactive. Requires VENDOR_DELETE permission.",
        parameters = {
            @Parameter(
                name = "vendorNumber",
                description = "Unique identifier of the vendor to delete",
                example = "VEND-001",
                required = true
            )
        }
    )
    @ApiResponse(
        responseCode = "204",
        description = "Vendor deleted successfully"
    )
    @ApiResponse(
        responseCode = "404",
        description = "Vendor not found",
        content = @Content(schema = @Schema(hidden = true))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid vendor number format",
        content = @Content(schema = @Schema(hidden = true))
    )
    @PreAuthorize("hasAuthority('VENDOR_DELETE')")
    public ResponseEntity<Void> deleteVendor(
            @PathVariable String vendorNumber) 
            throws ResourceNotFoundException {
        
        log.info("Deleting vendor with number: {}", vendorNumber);
        vendorService.deleteVendor(vendorNumber);
        log.info("Successfully deleted vendor: {}", vendorNumber);
        
        return ResponseEntity.noContent()
                .header("X-Vendor-Number", vendorNumber)
                .build();
    }

    @GetMapping(
        value = "/{vendorNumber}/h2h-eligibility", 
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        summary = "Check H2H eligibility",
        description = "Checks if a vendor is eligible for Host to Host (H2H) payments based on their bank details. Requires VENDOR_READ permission.",
        parameters = {
            @Parameter(
                name = "vendorNumber",
                description = "Vendor number to check for H2H eligibility",
                example = "VEND-001",
                required = true
            )
        }
    )
    @ApiResponse(
        responseCode = "200",
        description = "H2H eligibility check completed",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                type = "object",
                description = "Eligibility status and details",
                example = """
                {
                  "eligible": true,
                  "vendorNumber": "VEND-001",
                  "vendorName": "ABC Suppliers",
                  "message": "Vendor is eligible for H2H payments",
                  "checks": [
                    {
                      "check": "Bank Account Number",
                      "valid": true,
                      "message": "Bank account number is valid"
                    },
                    {
                      "check": "IFSC Code",
                      "valid": true,
                      "message": "IFSC code is valid"
                    },
                    {
                      "check": "Bank Name",
                      "valid": true,
                      "message": "Bank name is provided"
                    }
                  ]
                }
                """
            )
        )
    )
    @ApiResponse(
        responseCode = "404",
        description = "Vendor not found",
        content = @Content(schema = @Schema(hidden = true))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid vendor number format",
        content = @Content(schema = @Schema(hidden = true))
    )
    public ResponseEntity<Map<String, Object>> checkH2HEligibility(
            @PathVariable String vendorNumber) 
            throws ResourceNotFoundException {
        
        log.debug("Checking H2H eligibility for vendor: {}", vendorNumber);
        boolean isEligible = vendorService.isEligibleForH2H(vendorNumber);
        
        // Get vendor details for the response
        VendorResponseDTO vendor = vendorService.getVendorByVendorNumber(vendorNumber);
        
        // Build a detailed response
        Map<String, Object> response = Map.of(
            "eligible", isEligible,
            "vendorNumber", vendor.getVendorNumber(),
            "vendorName", vendor.getVendorName(),
            "message", isEligible ? "Vendor is eligible for H2H payments" : "Vendor is not eligible for H2H payments"
        );
        
        return ResponseEntity.ok(response);
    }

}
