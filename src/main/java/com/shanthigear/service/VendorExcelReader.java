package com.shanthigear.service;

import com.shanthigear.model.Vendor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for reading vendor data from Excel files.
 */
@Service
@Slf4j
public class VendorExcelReader {
    
    /**
     * Expected headers in the vendor master Excel file
     */
    private static final String[] EXPECTED_HEADERS = {
        "Vendor Number", "Vendor Name", "Vendor Site", "Pay Group",
        "Address Line1", "Address Line2", "Address line 3", "City", "State", "Pincode",
        "Account Number", "Bank Name", "IFSC Code", "Branch", "Email Address",
        "Creation Date", "Vendor Type", "Start Date Activity", "Attribute12", 
        "Attribute13", "Freight Terms Lookup Code", "Payment Method Lookup Code",
        "Bank Account Name", "Bank Account Num", "Attribute2", "Attribute3", "Ou"
    };
    
    /**
     * Required fields for H2H payments
     */
    private static final String[] REQUIRED_FIELDS = {
        "Vendor Number", "Vendor Name", "Bank Account Num", "Bank Name", "IFSC Code"
    };
    
    /**
     * Recommended fields for H2H payments
     */
    private static final String[] RECOMMENDED_FIELDS = {
        "Pay Group", "Email Address", "Bank Account Name", "Branch"
    };
    
    /**
     * Map to store header names to column indices (case-insensitive)
     */
    private final Map<String, Integer> headerMap = new HashMap<>();
    
    /**
     * Workbook instance for formula evaluation
     */
    private Workbook workbook;
    
    /**
     * Date formatter for parsing dates
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    

    /**
     * Reads vendor data from an Excel file.
     * 
     * @param file The Excel file to read from
     * @return A list of Vendor objects
     * @throws IOException If there's an error reading the file
     * @throws IllegalArgumentException If the file format is invalid
     */
    public List<Vendor> readVendors(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("The uploaded file is empty");
        }
        
        log.info("Starting to read vendor data from file: {}", file.getOriginalFilename());
        List<Vendor> vendors = new ArrayList<>();
        
        try (InputStream inputStream = file.getInputStream()) {
            // Initialize workbook and store it in the instance variable for formula evaluation
            this.workbook = new XSSFWorkbook(inputStream);
            
            try {
                Sheet sheet = workbook.getSheetAt(0);
                if (sheet == null) {
                    throw new IllegalArgumentException("The Excel file does not contain any sheets");
                }
                
                // Get header row
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    throw new IllegalArgumentException("The Excel file is empty");
                }
                
                // Initialize header map with column indices
                headerMap.clear();
                for (Cell cell : headerRow) {
                    if (cell != null && cell.getCellType() == CellType.STRING) {
                        String headerName = cell.getStringCellValue().trim();
                        if (!headerName.isEmpty()) {
                            headerMap.put(headerName.toLowerCase(), cell.getColumnIndex());
                        }
                    }
                }
                
                // Validate headers
                validateHeaders(headerRow);
                
                log.info("Processing {} data rows in the Excel file", sheet.getLastRowNum());
                int validRows = 0;
                int skippedRows = 0;
                
                // Process data rows
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null || isRowEmpty(row)) {
                        log.debug("Skipping empty row {}", i + 1);
                        skippedRows++;
                        continue;
                    }
                    
                    try {
                        Vendor vendor = mapRowToVendor(row);
                        if (vendor != null) {
                            vendors.add(vendor);
                            validRows++;
                        } else {
                            log.debug("Skipping row {}: No vendor data found", i + 1);
                            skippedRows++;
                        }
                    } catch (Exception e) {
                        log.warn("Error processing row {}: {}", i + 1, e.getMessage());
                        skippedRows++;
                    }
                    
                    // Log progress for large files
                    if (i > 0 && i % 100 == 0) {
                        log.info("Processed {} rows ({} valid, {} skipped)", i, validRows, skippedRows);
                    }
                }
                
                log.info("Completed processing: {} total rows, {} valid vendors, {} skipped rows", 
                        sheet.getLastRowNum(), validRows, skippedRows);
                
                return vendors;
                
            } finally {
                // Ensure workbook is closed properly
                try {
                    if (workbook != null) {
                        workbook.close();
                    }
                } catch (IOException e) {
                    log.warn("Error closing workbook: {}", e.getMessage());
                } finally {
                    this.workbook = null;
                }
            }
            
        } catch (Exception e) {
            log.error("Error reading Excel file: {}", e.getMessage(), e);
            throw new IOException("Failed to read Excel file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validates the header row against the expected headers.
     * @param headerRow The header row to validate
     * @throws IllegalArgumentException If required headers are missing
     */
    private void validateHeaders(Row headerRow) {
        if (headerRow == null) {
            throw new IllegalArgumentException("The Excel file is empty or has an invalid format");
        }
        
        // Convert arrays to lowercase sets for case-insensitive comparison
        Set<String> expectedHeaders = Arrays.stream(EXPECTED_HEADERS)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
                
        Set<String> requiredHeaders = Arrays.stream(REQUIRED_FIELDS)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
                
        Set<String> recommendedHeaders = Arrays.stream(RECOMMENDED_FIELDS)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        
        // Track which headers we found
        Set<String> foundHeaders = new HashSet<>();
        
        // Process the header row
        for (Cell cell : headerRow) {
            if (cell != null && cell.getCellType() == CellType.STRING) {
                String headerName = cell.getStringCellValue().trim();
                if (!headerName.isEmpty()) {
                    foundHeaders.add(headerName.toLowerCase());
                }
            }
        }
        
        // Check for missing required headers
        List<String> missingRequired = new ArrayList<>();
        for (String required : requiredHeaders) {
            if (!foundHeaders.contains(required.toLowerCase())) {
                missingRequired.add(required);
            }
        }
        
        // Check for missing recommended headers
        List<String> missingRecommended = new ArrayList<>();
        for (String recommended : recommendedHeaders) {
            if (!foundHeaders.contains(recommended.toLowerCase())) {
                missingRecommended.add(recommended);
            }
        }
        
        // Check for extra headers (not in expected headers)
        List<String> extraHeaders = new ArrayList<>();
        for (String found : foundHeaders) {
            boolean isExpected = false;
            for (String expected : expectedHeaders) {
                if (expected.equalsIgnoreCase(found)) {
                    isExpected = true;
                    break;
                }
            }
            if (!isExpected) {
                extraHeaders.add(found);
            }
        }
        
        // Build error message if needed
        StringBuilder message = new StringBuilder();
        
        // Add missing required headers to message
        if (!missingRequired.isEmpty()) {
            message.append("Missing required headers: ").append(String.join(", ", missingRequired));
        }
        
        // Add missing recommended headers to message
        if (!missingRecommended.isEmpty()) {
            if (message.length() > 0) {
                message.append("\n");
            }
            message.append("Missing recommended headers: ").append(String.join(", ", missingRecommended));
        }
        
        // Add extra headers to message
        if (!extraHeaders.isEmpty()) {
            if (message.length() > 0) {
                message.append("\n");
            }
            message.append("Unexpected headers found: ").append(String.join(", ", extraHeaders));
        }
        
        // If there are missing required headers, throw an exception
        if (!missingRequired.isEmpty()) {
            String errorMsg = message.toString();
            log.error("Header validation failed: {}", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        
        // Otherwise, just log warnings for missing recommended headers or extra headers
        if (message.length() > 0) {
            log.warn("Header validation warnings: {}", message.toString().replace("\n", " "));
        }
    }
    
    /**
     * Checks if a row is empty (all cells are null or empty).
     * @param row The row to check
     * @return true if the row is empty, false otherwise
     */
    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null) {
                switch (cell.getCellType()) {
                    case STRING:
                        if (!cell.getStringCellValue().trim().isEmpty()) {
                            return false;
                        }
                        break;
                    case NUMERIC:
                    case BOOLEAN:
                    case FORMULA:
                        return false;
                    case BLANK:
                    case _NONE:
                    case ERROR:
                        // Continue checking other cells
                        break;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Gets the string value from a cell, handling different cell types.
     * @param cell The cell to get the value from
     * @return The string value of the cell, or empty string if the cell is null or empty
     */
    private String getStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        try {
                            return cell.getLocalDateTimeCellValue().toLocalDate().format(DATE_FORMATTER);
                        } catch (Exception e) {
                            log.warn("Error parsing date value: {}", e.getMessage());
                            return "";
                        }
                    } else {
                        // Remove .0 from whole numbers for consistency
                        double num = cell.getNumericCellValue();
                        if (num == (long) num) {
                            return String.format("%.0f", num);
                        } else {
                            return String.valueOf(num);
                        }
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        // Evaluate the formula and get its value
                        CellValue cellValue = workbook.getCreationHelper()
                                .createFormulaEvaluator()
                                .evaluate(cell);
                                
                        if (cellValue != null) {
                            switch (cellValue.getCellType()) {
                                case STRING:
                                    return cellValue.getStringValue().trim();
                                case NUMERIC:
                                    if (DateUtil.isCellDateFormatted(cell)) {
                                        return cell.getLocalDateTimeCellValue().toLocalDate().format(DATE_FORMATTER);
                                    } else {
                                        double val = cellValue.getNumberValue();
                                        if (val == (long) val) {
                                            return String.format("%.0f", val);
                                        } else {
                                            return String.valueOf(val);
                                        }
                                    }
                                case BOOLEAN:
                                    return String.valueOf(cellValue.getBooleanValue());
                                default:
                                    return "";
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Error evaluating formula in cell: {}", e.getMessage());
                    }
                    return "";
                case BLANK:
                    return "";
                default:
                    return "";
            }
        } catch (Exception e) {
            log.warn("Error getting cell value: {}", e.getMessage());
            return "";
        }
    }
    
    /**
     * Gets the value from a row based on the header name.
     * @param row The row to get the value from
     * @param headerName The header name to look up (case-insensitive)
     * @return The string value of the cell, or null if not found
     */
    private String getValueByHeader(Row row, String headerName) {
        if (row == null || headerName == null || headerName.trim().isEmpty()) {
            return null;
        }
        
        // Normalize the header name for case-insensitive lookup
        String normalizedHeaderName = headerName.trim().toLowerCase();
        
        // Find the column index for the header (case-insensitive match)
        Integer colIndex = headerMap.get(normalizedHeaderName);
        
        if (colIndex == null) {
            log.trace("Header '{}' not found in header map", headerName);
            return null;
        }
        
        // Get the cell and return its string value
        try {
            Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell == null) {
                return null;
            }
            
            String value = getStringValue(cell);
            return value.isEmpty() ? null : value;
            
        } catch (Exception e) {
            log.warn("Error getting value for header '{}': {}", headerName, e.getMessage());
            return null;
        }
    }
    
    /**
     * Maps a row from the Excel file to a Vendor object.
     * @param row The row to map
     * @return A Vendor object, or null if the row is invalid
     */
    /* package-private */ Vendor mapRowToVendor(Row row) {
        if (row == null) {
            return null;
        }
        
        try {
            // Required fields for H2H payments
            String vendorNumber = getValueByHeader(row, "Vendor Number");
            String vendorName = getValueByHeader(row, "Vendor Name");
            String bankAccountNum = getValueByHeader(row, "Bank Account Num");
            String bankName = getValueByHeader(row, "Bank Name");
            String ifscCode = getValueByHeader(row, "IFSC Code");
            
            // Validate required fields
            if (vendorNumber == null || vendorName == null || bankAccountNum == null || 
                bankName == null || ifscCode == null) {
                log.warn("Skipping row {}: Missing required fields", row.getRowNum() + 1);
                return null;
            }
            
            // Create builder with required fields
            Vendor.VendorBuilder vendorBuilder = Vendor.builder()
                .vendorNumber(vendorNumber)
                .vendorName(vendorName)
                .bankAccountNum(bankAccountNum)
                .accountNumber(bankAccountNum) // Map to accountNumber as well
                .bankName(bankName)
                .ifscCode(ifscCode);
            
            // Set recommended fields for H2H
            String payGroup = getValueByHeader(row, "Pay Group");
            String email = getValueByHeader(row, "Email Address");
            String bankAccountName = getValueByHeader(row, "Bank Account Name");
            String branch = getValueByHeader(row, "Branch");
            
            if (payGroup != null && !payGroup.trim().isEmpty()) {
                vendorBuilder.payGroup(payGroup.trim());
            } else {
                log.warn("Row {}: Missing Pay Group - recommended for H2H payments", row.getRowNum() + 1);
            }
            
            if (email != null && !email.trim().isEmpty()) {
                vendorBuilder.emailAddress(email.trim());
            } else {
                log.warn("Row {}: Missing Email Address - recommended for H2H notifications", row.getRowNum() + 1);
            }
            
            if (bankAccountName != null && !bankAccountName.trim().isEmpty()) {
                vendorBuilder.bankAccountName(bankAccountName.trim());
            }
            
            if (branch != null && !branch.trim().isEmpty()) {
                vendorBuilder.branch(branch.trim());
            }
            
            // Map vendor details
            vendorBuilder.vendorSite(getValueByHeader(row, "Vendor Site"));
            
            // Address details
            vendorBuilder.addressLine1(getValueByHeader(row, "Address Line1"));
            vendorBuilder.addressLine2(getValueByHeader(row, "Address Line2"));
            vendorBuilder.addressLine3(getValueByHeader(row, "Address line 3"));
            vendorBuilder.city(getValueByHeader(row, "City"));
            vendorBuilder.state(getValueByHeader(row, "State"));
            vendorBuilder.pincode(getValueByHeader(row, "Pincode"));
            
            // Vendor type and start date
            vendorBuilder.vendorType(getValueByHeader(row, "Vendor Type"));
            
            String startDateStr = getValueByHeader(row, "Start Date Activity");
            if (startDateStr != null && !startDateStr.trim().isEmpty()) {
                try {
                    LocalDate startDate = LocalDate.parse(startDateStr.trim(), DATE_FORMATTER);
                    vendorBuilder.startDateActivity(startDate);
                } catch (DateTimeParseException e) {
                    log.warn("Row {}: Invalid date format for Start Date Activity: {}", row.getRowNum() + 1, startDateStr);
                }
            }
            
            // Set additional attributes
            vendorBuilder.attribute12(getValueByHeader(row, "Attribute12"));
            vendorBuilder.attribute13(getValueByHeader(row, "Attribute13"));
            vendorBuilder.attribute2(getValueByHeader(row, "Attribute2"));
            vendorBuilder.attribute3(getValueByHeader(row, "Attribute3"));
            vendorBuilder.freightTermsLookupCode(getValueByHeader(row, "Freight Terms Lookup Code"));
            vendorBuilder.paymentMethodLookupCode(getValueByHeader(row, "Payment Method Lookup Code"));
            
            // Validate required fields (already validated above, but checking again for completeness)
            List<String> validationWarnings = new ArrayList<>();
            
            if (vendorNumber == null || vendorNumber.trim().isEmpty()) {
                validationWarnings.add("Missing Vendor Number");
            }
            
            if (vendorName == null || vendorName.trim().isEmpty()) {
                validationWarnings.add("Missing Vendor Name");
            }
            
            if (bankAccountNum == null || bankAccountNum.trim().isEmpty()) {
                validationWarnings.add("Missing Bank Account Number");
            }
            
            if (bankName == null || bankName.trim().isEmpty()) {
                validationWarnings.add("Missing Bank Name");
            }
            
            if (ifscCode == null || ifscCode.trim().isEmpty()) {
                validationWarnings.add("Missing IFSC Code");
            }
            
            // Log any validation warnings
            if (!validationWarnings.isEmpty()) {
                log.warn("Validation warnings for vendor {}: {}", 
                    vendorNumber, String.join(", ", validationWarnings));
            }
            
            // Build the vendor
            Vendor vendor = vendorBuilder.build();
            
            // Store validation warnings in attribute13 if needed
            if (!validationWarnings.isEmpty()) {
                vendor.setAttribute13(String.join(", ", validationWarnings));
            }
            
            // Map additional fields
            String operatingUnit = getValueByHeader(row, "Ou");
            if (operatingUnit != null && !operatingUnit.trim().isEmpty()) {
                vendor.setOperatingUnit(operatingUnit.trim());
            }
            
            // Set attribute3 if needed
            String attribute3 = getValueByHeader(row, "Attribute3");
            if (attribute3 != null && !attribute3.trim().isEmpty()) {
                vendor.setAttribute3(attribute3.trim());
            }
            vendor.setFreightTermsLookupCode(getValueByHeader(row, "Freight Terms Lookup Code"));
            vendor.setPaymentMethodLookupCode(getValueByHeader(row, "Payment Method Lookup Code"));
            
            return vendor;
            
        } catch (Exception e) {
            log.error("Error mapping row {} to Vendor: {}", row.getRowNum() + 1, e.getMessage(), e);
            return null;
        }
    }
}
