package com.shanthigear.service;

import com.shanthigear.exception.ExcelProcessingException;
import com.shanthigear.model.PaymentStatus;
import com.shanthigear.model.Vendor;
import com.shanthigear.model.VendorPayment;
import com.shanthigear.repository.VendorPaymentRepository;
import com.shanthigear.repository.VendorRepository;
import jakarta.annotation.PostConstruct;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for processing Excel files containing vendor payment details.
 */
@Slf4j
@Service
public class ExcelProcessingService {

    private final String uploadDir;
    private final String paymentReferencePrefix;
    private final VendorRepository vendorRepository;
    private final VendorPaymentRepository vendorPaymentRepository;
    private final EmailService emailService;
    private final Map<String, Vendor> vendorCache = new ConcurrentHashMap<>();

    public ExcelProcessingService(VendorRepository vendorRepository,
                                VendorPaymentRepository vendorPaymentRepository,
                                EmailService emailService) {
        this.vendorRepository = vendorRepository;
        this.vendorPaymentRepository = vendorPaymentRepository;
        this.emailService = emailService;
        this.uploadDir = System.getProperty("user.home") + "/.vendor-payments/uploads";
        this.paymentReferencePrefix = "PAY";
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!");
        }
    }

    /**
     * Processes an Excel file containing vendor payment information.
     *
     * @param file The Excel file to process
     * @return List of processing results for each row
     * @throws ExcelProcessingException if the file is invalid or processing fails
     */
    @Transactional
    public List<PaymentProcessingResult> processVendorPayments(MultipartFile file) {
        // Validate input file
        if (file == null) {
            throw new ExcelProcessingException("File cannot be null");
        }
        
        if (file.isEmpty()) {
            throw new ExcelProcessingException("Uploaded file is empty");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".xlsx")) {
            throw new ExcelProcessingException("Only .xlsx files are supported");
        }
        
        log.info("Processing vendor payments from file: {}", originalFilename);
        List<PaymentProcessingResult> results = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new ExcelProcessingException("Excel file must contain at least one sheet");
            }
            
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new ExcelProcessingException("First sheet is missing in the Excel file");
            }
            
            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                throw new ExcelProcessingException("Excel sheet is empty");
            }

            // Skip header row
            Row headerRow = rowIterator.next();
            validateHeaderRow(headerRow);
            
            int rowNum = 1; // 0-based, but we've already read the header
            while (rowIterator.hasNext()) {
                rowNum++;
                Row row = rowIterator.next();
                
                try {
                    if (isRowEmpty(row)) {
                        log.debug("Skipping empty row {}", rowNum);
                        continue;
                    }
                    
                    log.debug("Processing row {}: {}", rowNum, rowToString(row));
                    PaymentProcessingResult result = processPaymentRow(row, vendorCache);
                    
                    if (!result.isSuccess()) {
                        String errorMsg = String.format("Failed to process row %d: %s", rowNum, result.getMessage());
                        log.warn(errorMsg);
                        throw new ExcelProcessingException(errorMsg);
                    }
                    
                    results.add(result);
                    log.info("Successfully processed payment for vendor {} with reference {}", 
                            result.getPayment().getVendorId(), 
                            result.getPayment().getPaymentReference());
                            
                } catch (Exception e) {
                    String errorMsg = String.format("Error processing row %d: %s", rowNum, e.getMessage());
                    log.error(errorMsg, e);
                    throw new ExcelProcessingException(errorMsg, e);
                }
            }

            if (results.isEmpty()) {
                log.warn("No valid payment records found in the file");
                throw new ExcelProcessingException("No valid payment records found in the file");
            }
            
            log.info("Successfully processed {} payment(s) from file: {}", results.size(), originalFilename);
            return results;
            
        } catch (IOException e) {
            String errorMsg = "Failed to read Excel file: " + e.getMessage();
            log.error(errorMsg, e);
            throw new ExcelProcessingException(errorMsg, e);
        } catch (ExcelProcessingException e) {
            // Re-throw our custom exceptions as-is
            throw e;
        } catch (Exception e) {
            String errorMsg = "Unexpected error processing Excel file: " + e.getMessage();
            log.error(errorMsg, e);
            throw new ExcelProcessingException(errorMsg, e);
        }
    }
    
    /**
     * Validates the header row of the Excel file.
     * 
     * @param headerRow The header row to validate
     * @throws ExcelProcessingException if the header is invalid
     */
    private void validateHeaderRow(Row headerRow) {
        if (headerRow == null) {
            throw new ExcelProcessingException("Header row is missing");
        }
        
        // Define expected headers with their positions (0-based index)
        Map<Integer, String> expectedHeaders = new LinkedHashMap<>();
        expectedHeaders.put(0, "Vendor ID");
        expectedHeaders.put(1, "Vendor Name");
        expectedHeaders.put(2, "Email");
        expectedHeaders.put(3, "Amount");
        expectedHeaders.put(4, "Payment Date");
        expectedHeaders.put(5, "Bank Account");
        expectedHeaders.put(6, "IFSC Code");
        expectedHeaders.put(7, "Bank Branch");
        expectedHeaders.put(8, "Invoice Number");
        expectedHeaders.put(9, "Vendor Type");
        
        // First, check if we have at least the minimum required columns
        int maxColumn = Collections.max(expectedHeaders.keySet());
        if (headerRow.getLastCellNum() <= maxColumn) {
            throw new ExcelProcessingException("Invalid number of columns. Expected at least " + 
                    (maxColumn + 1) + " columns but found " + headerRow.getLastCellNum());
        }
        
        // Validate each expected header
        for (Map.Entry<Integer, String> entry : expectedHeaders.entrySet()) {
            int colIndex = entry.getKey();
            String expectedHeader = entry.getValue();
            
            Cell cell = headerRow.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            String headerValue = (cell != null) ? cell.getStringCellValue().trim() : "";
            
            // Remove trailing asterisks and normalize whitespace for comparison
            String normalizedExpected = expectedHeader.replaceAll("\\*$", "").trim();
            String normalizedActual = headerValue.replaceAll("\\*$", "").trim();
            
            if (headerValue.isEmpty() || !normalizedActual.equalsIgnoreCase(normalizedExpected)) {
                throw new ExcelProcessingException("Invalid header in column " + (colIndex + 1) + ". " +
                        "Expected: '" + expectedHeader + "', " +
                        "Found: '" + headerValue + "'");
            }
        }
    }
    
    /**
     * Helper method to convert a row to a string for logging.
     */
    private String rowToString(Row row) {
        if (row == null) return "null";
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < row.getLastCellNum(); i++) {
            if (i > 0) sb.append(", ");
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            sb.append(cell == null ? "" : getStringCellValue(cell));
        }
        sb.append("]");
        return sb.toString();
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
            Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    /**
     * Processes a single row from the Excel file into a payment record.
     * 
     * @param row The row to process
     * @param vendorCache Cache for vendor lookups
     * @return Processing result with the payment or error message
     */
    private PaymentProcessingResult processPaymentRow(Row row, Map<String, Vendor> vendorCache) {
        try {
            // Extract and validate vendor ID (required)
            String vendorIdStr = getStringCellValue(row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL));
            if (vendorIdStr == null || vendorIdStr.trim().isEmpty()) {
                return new PaymentProcessingResult(null, "Vendor ID is required", false);
            }
            vendorIdStr = vendorIdStr.trim();
            
            // Parse vendor ID as Long
            Long vendorId;
            try {
                // Handle VEND001 format by removing non-numeric characters
                String numericId = vendorIdStr.replaceAll("\\D+", "");
                if (numericId.isEmpty()) {
                    return new PaymentProcessingResult(null, "Invalid Vendor ID format. Must contain numbers", false);
                }
                vendorId = Long.parseLong(numericId);
            } catch (NumberFormatException e) {
                return new PaymentProcessingResult(null, "Invalid Vendor ID format. Must be a number", false);
            }
            
            // Extract and validate amount (required) - Column 4 (index 3)
            String amountStr = getStringCellValue(row.getCell(3, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL));
            if (amountStr == null || amountStr.trim().isEmpty()) {
                return new PaymentProcessingResult(null, "Amount is required", false);
            }
            
            BigDecimal amount;
            try {
                amount = new BigDecimal(amountStr.trim().replaceAll(",", ""));
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    return new PaymentProcessingResult(null, "Amount must be greater than zero", false);
                }
            } catch (NumberFormatException e) {
                return new PaymentProcessingResult(null, "Invalid amount format. Must be a number", false);
            }
            
            // Extract and validate payment date (optional, defaults to today) - Column 5 (index 4)
            LocalDate paymentDate = LocalDate.now();
            String paymentDateStr = getStringCellValue(row.getCell(4, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL));
            if (paymentDateStr != null && !paymentDateStr.trim().isEmpty()) {
                try {
                    paymentDate = LocalDate.parse(paymentDateStr.trim());
                    // Validate that the date is not in the future
                    if (paymentDate.isAfter(LocalDate.now())) {
                        return new PaymentProcessingResult(null, "Payment date cannot be in the future", false);
                    }
                } catch (Exception e) {
                    return new PaymentProcessingResult(null, 
                        "Invalid payment date format. Please use YYYY-MM-DD", false);
                }
            }
            
            // Extract reference and notes (both optional)
            // Reference is in column 9 (index 8) - Invoice Number
            // Notes is in column 10 (index 9) - Vendor Type
            String reference = getStringCellValue(row.getCell(8, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL));
            String notes = row.getLastCellNum() > 9 ? 
                getStringCellValue(row.getCell(9, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)) : "";
                
            // Trim all string fields
            reference = reference != null ? reference.trim() : "";
            notes = notes != null ? notes.trim() : "";
            
            // Additional validations
            if (vendorId != null && vendorId.toString().length() > 50) {
                return new PaymentProcessingResult(null, "Vendor ID is too long (max 50 characters)", false);
            }
            
            if (reference.length() > 100) {
                return new PaymentProcessingResult(null, "Reference is too long (max 100 characters)", false);
            }
            
            if (notes.length() > 500) {
                return new PaymentProcessingResult(null, "Notes are too long (max 500 characters)", false);
            }
            
            log.debug("Processed row - Vendor: {}, Amount: {}, Date: {}, Ref: {}", 
                    vendorId, amount, paymentDate, reference);
                    
            // Process the payment with validated data
            return processPayment(row, vendorId.toString(), amount, paymentDate, reference, notes);
            
        } catch (Exception e) {
            log.error("Unexpected error processing row: " + e.getMessage(), e);
            return new PaymentProcessingResult(null, "Error processing payment: " + e.getMessage(), false);
        }
    }

    /**
     * Processes a payment with the given details.
     * 
     * @param vendorId The vendor ID (required)
     * @param amount The payment amount (must be positive)
     * @param paymentDate The payment date (required, not in the future)
     * @param reference Optional payment reference
     * @param notes Optional payment notes
     * @return Processing result with the saved payment or error message
     */
    private PaymentProcessingResult processPayment(Row row, String vendorId, BigDecimal amount, 
            LocalDate paymentDate, String reference, String notes) {
        
        // Input validation (should be redundant due to prior validation, but good practice)
        if (vendorId == null || vendorId.trim().isEmpty()) {
            return new PaymentProcessingResult(null, "Vendor ID is required", false);
        }
        
        String formattedVendorId = "";
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return new PaymentProcessingResult(null, "Amount must be greater than zero", false);
        }
        
        if (paymentDate == null) {
            paymentDate = LocalDate.now();
        } else if (paymentDate.isAfter(LocalDate.now())) {
            return new PaymentProcessingResult(null, "Payment date cannot be in the future", false);
        }
        
        // Sanitize inputs and create final variables for use in lambda
        final String vendorIdStr = vendorId != null ? vendorId.trim() : "";
        final String referenceFinal = reference != null ? reference.trim() : "";
        final String notesFinal = notes != null ? notes.trim() : "";
        
        if (vendorIdStr.isEmpty()) {
            return new PaymentProcessingResult(null, "Vendor ID cannot be empty", false);
        }
        
        VendorPayment payment = new VendorPayment();
        String paymentReference = referenceFinal.isEmpty() ? generateReferenceNumber() : referenceFinal;
        
        try {
            // Format vendor ID if it doesn't have the VEND prefix
            formattedVendorId = vendorIdStr.startsWith("VEND") ? vendorIdStr : "VEND" + vendorIdStr;
            log.debug("Looking up vendor with vendorId: {}", formattedVendorId);
            
            // First try to find the vendor by vendor number
            Optional<Vendor> existingVendor = vendorRepository.findByVendorNumber(formattedVendorId);
            Vendor vendor;
            
            if (existingVendor.isPresent()) {
                vendor = existingVendor.get();
                log.debug("Found existing vendor: {}", vendor.getVendorNumber());
            } else {
                log.info("Vendor with vendor number {} not found, creating new vendor record", formattedVendorId);
                vendor = Vendor.builder()
                    .vendorNumber(formattedVendorId)
                    .vendorName("New Vendor - " + vendorIdStr)
                    .vendorSite("DEFAULT")  // Required field
                    .payGroup("DEFAULT")     // Required field
                    .emailAddress("vendor" + vendorIdStr + "@example.com")
                    .addressLine1("Address not provided")
                    .bankAccountNum("ACCT" + vendorIdStr)
                    .accountNumber("ACCT" + vendorIdStr)  // Required field
                    .bankName("DEFAULT BANK")  // Required field
                    .ifscCode("IFSC" + vendorIdStr)
                    .city("City")
                    .state("State")
                    .pincode("123456")
                    .paymentMethodLookupCode("CHECK")  // Required field
                    .creationDate(LocalDate.now())
                    .build();
                
                // Save the new vendor
                vendor = vendorRepository.save(vendor);
                log.info("Created new vendor with vendor number: {}", vendor.getVendorNumber());
            }
            
            // Update vendor details from the Excel row if available
            boolean vendorUpdated = false;
            if (row != null) {
                String vendorName = getStringCellValue(row.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)); // Column 2: Vendor Name
                String email = getStringCellValue(row.getCell(2, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)); // Column 3: Email
                String bankAccount = getStringCellValue(row.getCell(5, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)); // Column 6: Bank Account
                String ifscCode = getStringCellValue(row.getCell(6, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)); // Column 7: IFSC Code
                // Bank Branch and Vendor Type columns are not currently used
                getStringCellValue(row.getCell(7, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)); // Column 8: Bank Branch
                getStringCellValue(row.getCell(9, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)); // Column 10: Vendor Type
                
                // Update vendor details if they're not already set or are using default values
                if (vendorName != null && !vendorName.trim().isEmpty() && 
                    (vendor.getVendorName() == null || vendor.getVendorName().startsWith("New Vendor -"))) {
                    vendor.setVendorName(vendorName.trim());
                    vendorUpdated = true;
                }
                if (email != null && !email.trim().isEmpty() && 
                    (vendor.getEmailAddress() == null || vendor.getEmailAddress().endsWith("@example.com"))) {
                    vendor.setEmailAddress(email.trim());
                    vendorUpdated = true;
                }
                if (bankAccount != null && !bankAccount.trim().isEmpty() && 
                    (vendor.getBankAccountNum() == null || vendor.getBankAccountNum().startsWith("ACCT"))) {
                    vendor.setBankAccountNum(bankAccount.trim());
                    vendorUpdated = true;
                }
                if (ifscCode != null && !ifscCode.trim().isEmpty() && 
                    (vendor.getIfscCode() == null || vendor.getIfscCode().startsWith("IFSC"))) {
                    vendor.setIfscCode(ifscCode.trim());
                    vendorUpdated = true;
                }
                // Add other vendor fields as needed
                
                if (vendorUpdated) {
                    vendor = vendorRepository.save(vendor);
                }
            }
            
            // Extract bank details from the row if available
            String bankAccount = null;
            String ifscCode = null;
            String invoiceNumber = null;
            
            if (row != null) {
                bankAccount = getStringCellValue(row.getCell(5, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)); // Column 6: Bank Account
                ifscCode = getStringCellValue(row.getCell(6, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)); // Column 7: IFSC Code
                invoiceNumber = getStringCellValue(row.getCell(8, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)); // Column 9: Invoice Number
            }
            
            // Create payment record with all required fields
            payment.setVendor(vendor);
            payment.setVendorName(vendor.getVendorName());
            payment.setVendorEmail(vendor.getEmailAddress());
            payment.setAmount(amount);
            payment.setPaymentDate(paymentDate);
            payment.setPaymentReference(paymentReference);
            payment.setReferenceNumber(paymentReference); // Using payment reference as reference number
            payment.setStatus(PaymentStatus.PENDING);
            payment.setCreatedAt(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());
            
            // Set optional fields if available
            if (bankAccount != null && !bankAccount.trim().isEmpty()) {
                payment.setBankAccount(bankAccount.trim());
            }
            if (ifscCode != null && !ifscCode.trim().isEmpty()) {
                payment.setIfscCode(ifscCode.trim());
            }
            if (invoiceNumber != null && !invoiceNumber.trim().isEmpty()) {
                payment.setInvoiceNumber(invoiceNumber.trim());
            }
            if (notesFinal != null && !notesFinal.trim().isEmpty()) {
                payment.setNotes(notesFinal);
            }
            
            log.debug("Saving payment: {}", payment);
            VendorPayment savedPayment = vendorPaymentRepository.save(payment);
            
            // Log success
            log.info("Successfully processed payment. Reference: {}, Vendor: {}, Amount: {}", 
                    savedPayment.getPaymentReference(), 
                    savedPayment.getVendorId(), 
                    savedPayment.getAmount());
            
            // Send notification asynchronously
            try {
                sendPaymentNotification(savedPayment);
            } catch (Exception e) {
                // Log but don't fail the operation if notification fails
                log.error("Failed to send payment notification for reference: " + 
                        savedPayment.getPaymentReference(), e);
            }
            
            return new PaymentProcessingResult(savedPayment, "Payment processed successfully", true);
            
        } catch (Exception e) {
            String errorMsg = String.format("Failed to process payment. Reference: %s, Vendor: %s, Error: %s",
                    paymentReference, formattedVendorId, e.getMessage());
            log.error(errorMsg, e);
            return new PaymentProcessingResult(null, "Error processing payment: " + e.getMessage(), false);
        }
    }

    private String generateReferenceNumber() {
        return paymentReferencePrefix + "-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    /**
     * Safely extracts a string value from an Excel cell, handling all cell types.
     * 
     * @param cell The cell to extract the value from
     * @return The string representation of the cell value, or empty string if the cell is null or empty
     */
    private String getStringCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                    
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        // Handle date values
                        try {
                            LocalDate date = cell.getLocalDateTimeCellValue().toLocalDate();
                            return date.toString();
                        } catch (Exception e) {
                            // Fallback to numeric if date parsing fails
                            double numValue = cell.getNumericCellValue();
                            // Check if it's an integer value
                            if (numValue == (long) numValue) {
                                return String.valueOf((long) numValue);
                            } else {
                                // Format to remove any trailing .0 for whole numbers
                                return String.format("%.2f", numValue).replaceAll("\\.?0+$", "");
                            }
                        }
                    } else {
                        // Handle regular numeric values
                        double numValue = cell.getNumericCellValue();
                        // Check if it's an integer value
                        if (numValue == (long) numValue) {
                            return String.valueOf((long) numValue);
                        } else {
                            // Format to 2 decimal places for non-integer values
                            return String.format("%.2f", numValue);
                        }
                    }
                    
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                    
                case FORMULA:
                    // Handle formula cells by evaluating them
                    try {
                        switch (cell.getCachedFormulaResultType()) {
                            case STRING:
                                return cell.getStringCellValue().trim();
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                                } else {
                                    double numValue = cell.getNumericCellValue();
                                    return numValue == (long) numValue ? 
                                        String.valueOf((long) numValue) : String.valueOf(numValue);
                                }
                            case BOOLEAN:
                                return String.valueOf(cell.getBooleanCellValue());
                            case ERROR:
                                return "#ERROR";
                            default:
                                return cell.getCellFormula();
                        }
                    } catch (Exception e) {
                        log.warn("Error evaluating formula in cell: " + cell.getAddress(), e);
                        return cell.getCellFormula();
                    }
                    
                case BLANK:
                    return "";
                    
                case ERROR:
                    return "#ERROR";
                    
                case _NONE:
                default:
                    return "";
            }
        } catch (Exception e) {
            log.warn("Error reading cell value at {}: {}", 
                    cell.getAddress().formatAsString(), e.getMessage());
            return "";
        }
    }

    @Async
    public void sendPaymentNotification(VendorPayment payment) {
        if (emailService != null && payment != null && payment.getVendorEmail() != null) {
            try {
                // Send email notification
                String subject = "Payment Processed: " + payment.getPaymentReference();
                String content = String.format("Payment of %s has been processed successfully.\nReference: %s",
                    payment.getAmount(), payment.getPaymentReference());
                
                emailService.sendSimpleEmail(payment.getVendorEmail(), subject, content);
                log.info("Payment notification sent for payment: {}", payment.getPaymentReference());
            } catch (Exception e) {
                log.error("Failed to send payment notification for payment: {}", 
                    payment.getPaymentReference(), e);
            }
        } else if (payment != null) {
            log.warn("Email service not available or missing email address. Payment processed successfully. Reference: {}", 
                   payment.getPaymentReference());
        }
    }

    /**
     * Loads the payment template file, creating it if it doesn't exist.
     *
     * @return Resource representing the template file
     * @throws ExcelProcessingException if the template cannot be created or accessed
     */
    public Resource loadTemplate() {
        try {
            Path templatePath = createTemplateFileIfNotExists();
            log.debug("Loading template from: {}", templatePath);
            
            Resource resource = new UrlResource(templatePath.toUri());
            
            if (!resource.exists()) {
                throw new ExcelProcessingException("Template file does not exist: " + templatePath);
            }
            
            if (!resource.isReadable()) {
                throw new ExcelProcessingException("Template file is not readable: " + templatePath);
            }
            
            log.info("Successfully loaded template from: {}", templatePath);
            return resource;
            
        } catch (MalformedURLException e) {
            throw new ExcelProcessingException("Invalid template file path", e);
        } catch (Exception e) {
            throw new ExcelProcessingException("Failed to load template: " + e.getMessage(), e);
        }
    }

    private Path createTemplateFileIfNotExists() {
        try {
            // Ensure upload directory exists
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath);
            }
            
            Path templatePath = uploadPath.resolve("payment_template.xlsx");
            if (!Files.exists(templatePath)) {
                log.info("Template file not found, creating new one at: {}", templatePath);
                createTemplateFile();
            } else {
                log.debug("Using existing template file at: {}", templatePath);
            }
            
            // Verify the file is readable
            if (!Files.isReadable(templatePath)) {
                throw new ExcelProcessingException("Template file is not readable: " + templatePath);
            }
            
            return templatePath;
        } catch (IOException e) {
            throw new ExcelProcessingException("Failed to create or access template file", e);
        }
    }

    private void createTemplateFile() {
        Path templatePath = Paths.get(uploadDir, "payment_template.xlsx");
        
        // Ensure parent directory exists
        try {
            Files.createDirectories(templatePath.getParent());
        } catch (IOException e) {
            throw new ExcelProcessingException("Failed to create template directory: " + templatePath.getParent(), e);
        }
        
        // Create a temporary file first, then move it to the final location atomically
        Path tempFile;
        try {
            tempFile = Files.createTempFile(templatePath.getParent(), "temp_template_", ".xlsx");
        } catch (IOException e) {
            throw new ExcelProcessingException("Failed to create temporary template file", e);
        }
        
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create a bold font for headers
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            
            Sheet sheet = workbook.createSheet("Vendor Payments");
            
            // Create header row with styling
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Vendor ID*", "Amount*", "Payment Date (YYYY-MM-DD)", "Reference", "Notes"};
            
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Create example data row
            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("VENDOR123");
            exampleRow.createCell(1).setCellValue(1000.50);
            exampleRow.createCell(2).setCellValue(LocalDate.now().toString());
            exampleRow.createCell(3).setCellValue("INV-12345");
            exampleRow.createCell(4).setCellValue("Monthly payment");
            
            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
                // Add some padding
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 1500, 20 * 256));
            }
            
            // Write to temp file first
            try (FileOutputStream fileOut = new FileOutputStream(tempFile.toFile())) {
                workbook.write(fileOut);
            }
            
            // Move temp file to final location atomically
            try {
                Files.move(tempFile, templatePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                log.info("Successfully created template file at: {}", templatePath);
            } catch (IOException e) {
                throw new ExcelProcessingException("Failed to move template file to final location: " + templatePath, e);
            }
            
        } catch (IOException e) {
            // Clean up temp file if it exists
            try {
                if (tempFile != null) {
                    Files.deleteIfExists(tempFile);
                }
            } catch (IOException ex) {
                log.warn("Failed to delete temporary file: " + tempFile, ex);
            }
            throw new ExcelProcessingException("Failed to create template file: " + e.getMessage(), e);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class PaymentProcessingResult {
        private final VendorPayment payment;
        private final String message;
        private final boolean success;
        
        public String getMessage() { 
            return message != null ? message : ""; 
        }
        
        public boolean isSuccess() { 
            return success; 
        }
        
        @Override
        public String toString() {
            return "PaymentProcessingResult{" +
                   "payment=" + (payment != null ? payment.getPaymentReference() : "null") +
                   ", message='" + message + '\'' +
                   ", success=" + success +
                   '}';
        }
    }
}
