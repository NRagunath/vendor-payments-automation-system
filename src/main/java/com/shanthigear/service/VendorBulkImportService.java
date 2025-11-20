package com.shanthigear.service;

import com.shanthigear.dto.VendorImportDTO;
import com.shanthigear.dto.BulkImportResponse;
import com.shanthigear.model.Vendor;
import com.shanthigear.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import lombok.Data;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import java.time.LocalDate;

import org.apache.commons.lang3.StringUtils;

import com.shanthigear.exception.ExcelProcessingException;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendorBulkImportService {
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000L;
    private final VendorRepository vendorRepository;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final TransactionTemplate transactionTemplate;
    
    @Value("${vendor.import.batch-size:50}")
    private int batchSize;
    
    @Value("${vendor.import.max-threads:10}")
    private int maxThreads;
    
    public BulkImportResponse processBulkImport(MultipartFile file) throws IOException {
        long startTime = System.currentTimeMillis();
        log.info("Starting bulk import process for file: {}", file.getOriginalFilename());
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            // Process sheet in streaming mode to handle large files
            Sheet sheet = workbook.getSheetAt(0);
            int totalRows = sheet.getLastRowNum();
            log.info("Found {} rows to process", totalRows);
            
            // Initialize counters
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            List<String> errors = Collections.synchronizedList(new ArrayList<>());
            
            // Process in chunks to manage memory
            int chunkSize = 1000; // Process 1000 rows at a time
            for (int i = 1; i <= totalRows; i += chunkSize) {
                int end = Math.min(i + chunkSize - 1, totalRows);
                log.info("Processing rows {} to {}", i, end);
                
                // Prepare batch of rows
                List<Row> batch = new ArrayList<>();
                for (int j = i; j <= end; j++) {
                    batch.add(sheet.getRow(j));
                }
                
                List<BatchResult> results = processBatch(batch);
                
                // Update counters
                for (BatchResult result : results) {
                    successCount.addAndGet(result.getSuccessCount());
                    failureCount.addAndGet(result.getFailureCount());
                    errors.addAll(result.getErrors());
                }
            }
            
            long endTime = System.currentTimeMillis();
            log.info("Completed bulk import in {} ms. Success: {}, Failures: {}", 
                    (endTime - startTime), successCount.get(), failureCount.get());
            
            return BulkImportResponse.builder()
                    .totalRecords(totalRows)
                    .successCount(successCount.get())
                    .failureCount(failureCount.get())
                    .errors(errors.size() > 100 ? errors.subList(0, 100) : errors) // Limit to 100 errors
                    .build();
                    
        } catch (Exception e) {
            log.error("Bulk import failed", e);
            throw new RuntimeException("Failed to process bulk import: " + e.getMessage(), e);
        }
    }
    

    private List<BatchResult> processBatch(List<Row> batch) {
        List<BatchResult> results = new ArrayList<>();
        List<CompletableFuture<BatchResult>> futures = batch.stream()
            .map(row -> CompletableFuture.supplyAsync(() -> {
                AtomicInteger successCount = new AtomicInteger(0);
                List<String> errors = Collections.synchronizedList(new ArrayList<>());
                
                processRow(row, successCount, new AtomicInteger(0), errors);
                
                return new BatchResult(successCount.get(), errors);
            }, taskExecutor))
            .collect(Collectors.toList());
        
        // Wait for all futures to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture<?>[0])
        );
        
        try {
            allOf.get(); // Wait for all futures to complete
            
            // Collect results
            for (CompletableFuture<BatchResult> future : futures) {
                results.add(future.get());
            }
        } catch (Exception e) {
            log.error("Error processing batch: {}", e.getMessage(), e);
            // Add error result for failed batch
            results.add(new BatchResult(0, Collections.singletonList("Error processing batch: " + e.getMessage())));
        }
        
        return results;
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void processRow(Row row, AtomicInteger successCount, AtomicInteger failureCount, List<String> errors) {
        if (row == null) return;
        
        DataFormatter formatter = new DataFormatter();
        String vendorId = formatter.formatCellValue(row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL));
        
        try {
            // Parse DTO from row with all columns from vendor master Excel
            VendorImportDTO dto = VendorImportDTO.builder()
                .vendorNumber(vendorId)
                .vendorName(getCellValue(row, 1, formatter))
                .vendorSite(getCellValue(row, 2, formatter))
                .payGroup(getCellValue(row, 3, formatter))
                .addressLine1(getCellValue(row, 4, formatter))
                .addressLine2(getCellValue(row, 5, formatter))
                .addressLine3(getCellValue(row, 6, formatter))
                .city(getCellValue(row, 7, formatter))
                .state(getCellValue(row, 8, formatter))
                .pincode(getCellValue(row, 9, formatter))
                .bankAccountNum(getCellValue(row, 10, formatter))
                .bankName(getCellValue(row, 11, formatter))
                .ifscCode(getCellValue(row, 12, formatter))
                .branch(getCellValue(row, 13, formatter))
                .emailAddress(getCellValue(row, 14, formatter))
                .creationDate(getCellValue(row, 15, formatter))
                .vendorType(getCellValue(row, 16, formatter))
                .startDateActivity(getCellValue(row, 17, formatter))
                .attribute12(getCellValue(row, 18, formatter))
                .attribute13(getCellValue(row, 19, formatter))
                .freightTermsLookupCode(getCellValue(row, 20, formatter))
                .paymentMethodLookupCode(getCellValue(row, 21, formatter))
                .bankAccountName(getCellValue(row, 22, formatter))
                .attribute2(getCellValue(row, 23, formatter))
                .attribute3(getCellValue(row, 24, formatter))
                .build();
                
            // Validate required fields
            List<String> validationErrors = dto.validate();
            if (!validationErrors.isEmpty()) {
                String errorMsg = String.format("Row %d: %s", row.getRowNum() + 1, 
                        String.join(", ", validationErrors));
                errors.add(errorMsg);
                failureCount.incrementAndGet();
                return;
            }
            
            // Validation already done above using dto.validate()
            
            // Process with retry logic
            boolean[] success = {false};
            int attempt = 0;
            while (!success[0] && attempt < MAX_RETRIES) {
                try {
                    transactionTemplate.execute(status -> {
                        try {
                            // Find existing vendor or create a new one
                            Vendor vendor = vendorRepository.findByVendorNumber(dto.getVendorNumber())
                                .orElseGet(() -> Vendor.builder().build());
                            
                            // Create or update vendor using builder
                            Vendor.VendorBuilder vendorBuilder = Vendor.builder()
                                // Basic information
                                .vendorNumber(dto.getVendorNumber())
                                .vendorName(dto.getVendorName())
                                .vendorSite(dto.getVendorSite())
                                .payGroup(dto.getPayGroup())
                                
                                // Address
                                .addressLine1(dto.getAddressLine1())
                                .addressLine2(dto.getAddressLine2())
                                .addressLine3(dto.getAddressLine3())
                                .city(dto.getCity())
                                .state(dto.getState())
                                .pincode(dto.getPincode())
                                
                                // Bank details
                                .bankAccountNum(dto.getBankAccountNum())
                                .accountNumber(dto.getBankAccountNum()) // Map to accountNumber as well
                                .bankName(dto.getBankName())
                                .ifscCode(dto.getIfscCode())
                                .branch(dto.getBranch())
                                .bankAccountName(dto.getBankAccountName())
                                
                                // Contact information
                                .emailAddress(dto.getEmailAddress())
                                .vendorType(dto.getVendorType())
                                
                                // Additional attributes
                                .freightTermsLookupCode(dto.getFreightTermsLookupCode())
                                .paymentMethodLookupCode(dto.getPaymentMethodLookupCode())
                                .attribute12(dto.getAttribute12())
                                .attribute13(dto.getAttribute13())
                                .attribute2(dto.getAttribute2())
                                .attribute3(dto.getAttribute3())
                                .operatingUnit(dto.getOperatingUnit())
                                
                                // Set creation date for new vendors
                                .creationDate(vendor.getCreationDate() != null ? 
                                    vendor.getCreationDate() : LocalDate.now());
                            
                            // Parse and set start date activity if present
                            if (StringUtils.isNotBlank(dto.getStartDateActivity())) {
                                try {
                                    LocalDate startDate = LocalDate.parse(dto.getStartDateActivity());
                                    vendorBuilder.startDateActivity(startDate);
                                } catch (Exception e) {
                                    log.warn("Invalid start date format for vendor {}: {}", 
                                            dto.getVendorNumber(), dto.getStartDateActivity());
                                }
                            }
                            
                            // Build the vendor
                            Vendor updatedVendor = vendorBuilder.build();
                            
                            // For existing vendors, preserve the creation date
                            if (vendor.getVendorNumber() != null) {
                                updatedVendor.setCreationDate(vendor.getCreationDate());
                            }
                            
                            // Save the vendor
                            Vendor savedVendor = vendorRepository.save(updatedVendor);
                            
                            log.info("Successfully processed vendor: {}", savedVendor.getVendorNumber());
                            successCount.incrementAndGet();
                            success[0] = true;
                            return null;
                        } catch (Exception e) {
                            log.error("Error processing vendor {}: {}", dto.getVendorNumber(), e.getMessage(), e);
                            throw new ExcelProcessingException("Error processing vendor data: " + e.getMessage());
                        }
                    });
                    
                    // If we reach here, the transaction was successful
                    break;
                } catch (Exception e) {
                    attempt++;
                    if (attempt >= MAX_RETRIES) {
                        log.error("Failed to process vendor {} after {} attempts: {}", 
                                dto.getVendorNumber(), MAX_RETRIES, e.getMessage());
                        errors.add(String.format("Row %d: Failed to process vendor %s: %s", 
                                row.getRowNum() + 1, dto.getVendorNumber(), e.getMessage()));
                        failureCount.incrementAndGet();
                    } else {
                        try {
                            Thread.sleep(RETRY_DELAY_MS * (attempt + 1));
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new ExcelProcessingException("Processing interrupted", ie);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            String errorMsg = String.format("Error processing row %d (vendor ID: %s): %s",
                    row.getRowNum() + 1, vendorId, e.getMessage());
            errors.add(errorMsg);
            failureCount.incrementAndGet();
            log.error(errorMsg, e);
        }
    }
    
    private String getCellValue(Row row, int cellNum, DataFormatter formatter) {
        return getCellValue(row, cellNum, formatter, "");
    }
    
    private String getCellValue(Row row, int cellNum, DataFormatter formatter, String defaultValue) {
        if (row == null) return defaultValue;
        Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell != null ? formatter.formatCellValue(cell).trim() : defaultValue;
    }
    
    @Data
    public static class BatchResult {
        private final int successCount;
        private final List<String> errors;
        
        public BatchResult(int successCount, List<String> errors) {
            this.successCount = successCount;
            this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
        }
        
        public int getSuccessCount() {
            return successCount;
        }
        
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }
        
        public int getFailureCount() {
            return errors.size();
        }
    }
}
