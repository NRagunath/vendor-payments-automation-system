package com.shanthigear.service;

import com.shanthigear.exception.ImportException;
import com.shanthigear.model.Vendor;
import com.shanthigear.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Service for importing vendor data from Excel files.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VendorImportService {
    
    private final VendorExcelReader vendorExcelReader;
    private final VendorRepository vendorRepository;
    
    /**
     * Import vendors from an Excel file.
     * 
     * @param file The Excel file containing vendor data
     * @return ImportResult containing the number of vendors imported and any errors
     * @throws ImportException If there's an error processing the file
     */
    @Transactional
    public ImportResult importVendors(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImportException("File is empty or not provided");
        }
        
        try {
            // Read vendors from Excel
            List<Vendor> vendors = vendorExcelReader.readVendors(file);
            
            if (vendors.isEmpty()) {
                return new ImportResult(0, 0, "No valid vendor records found in the file");
            }
            
            int importedCount = 0;
            int errorCount = 0;
            
            // Process each vendor
            for (Vendor vendor : vendors) {
                try {
                    saveOrUpdateVendor(vendor);
                    importedCount++;
                } catch (Exception e) {
                    errorCount++;
                    log.error("Error importing vendor {}: {}", vendor.getVendorNumber(), e.getMessage(), e);
                }
            }
            
            String message = String.format(
                "Import completed. Success: %d, Failed: %d", 
                importedCount, errorCount);
            
            return new ImportResult(importedCount, errorCount, message);
            
        } catch (IOException e) {
            throw new ImportException("Error reading the file: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ImportException("Error processing vendor import: " + e.getMessage(), e);
        }
    }
    
    /**
     * Save or update a vendor based on vendor ID.
     */
    private void saveOrUpdateVendor(Vendor vendor) {
        try {
            // Check if vendor exists
            vendorRepository.findByVendorNumber(vendor.getVendorNumber())
                .ifPresentOrElse(
                    existingVendor -> updateExistingVendor(existingVendor, vendor),
                    () -> vendorRepository.save(vendor)
                );
        } catch (DataIntegrityViolationException e) {
            throw new ImportException("Data integrity violation for vendor " + vendor.getVendorNumber() + ": " + e.getMessage());
        }
    }
    
    /**
     * Update existing vendor with new data.
     */
    private void updateExistingVendor(Vendor existingVendor, Vendor newVendor) {
        // Update basic info
        if (newVendor.getVendorName() != null) {
            existingVendor.setVendorName(newVendor.getVendorName());
        }
        if (newVendor.getEmailAddress() != null) {
            existingVendor.setEmailAddress(newVendor.getEmailAddress());
        }
        
        // Update bank details
        if (newVendor.getBankAccountNum() != null) {
            existingVendor.setBankAccountNum(newVendor.getBankAccountNum());
        }
        if (newVendor.getAccountNumber() != null) {
            existingVendor.setAccountNumber(newVendor.getAccountNumber());
        }
        if (newVendor.getBankName() != null) {
            existingVendor.setBankName(newVendor.getBankName());
        }
        if (newVendor.getIfscCode() != null) {
            existingVendor.setIfscCode(newVendor.getIfscCode());
        }
        if (newVendor.getBranch() != null) {
            existingVendor.setBranch(newVendor.getBranch());
        }
        if (newVendor.getBankAccountName() != null) {
            existingVendor.setBankAccountName(newVendor.getBankAccountName());
        }
        
        // Update address
        if (newVendor.getAddressLine1() != null) {
            existingVendor.setAddressLine1(newVendor.getAddressLine1());
        }
        if (newVendor.getAddressLine2() != null) {
            existingVendor.setAddressLine2(newVendor.getAddressLine2());
        }
        if (newVendor.getAddressLine3() != null) {
            existingVendor.setAddressLine3(newVendor.getAddressLine3());
        }
        if (newVendor.getCity() != null) {
            existingVendor.setCity(newVendor.getCity());
        }
        if (newVendor.getState() != null) {
            existingVendor.setState(newVendor.getState());
        }
        if (newVendor.getPincode() != null) {
            existingVendor.setPincode(newVendor.getPincode());
        }
        
        // Update other fields
        if (newVendor.getVendorSite() != null) {
            existingVendor.setVendorSite(newVendor.getVendorSite());
        }
        if (newVendor.getPayGroup() != null) {
            existingVendor.setPayGroup(newVendor.getPayGroup());
        }
        if (newVendor.getVendorType() != null) {
            existingVendor.setVendorType(newVendor.getVendorType());
        }
        if (newVendor.getFreightTermsLookupCode() != null) {
            existingVendor.setFreightTermsLookupCode(newVendor.getFreightTermsLookupCode());
        }
        if (newVendor.getPaymentMethodLookupCode() != null) {
            existingVendor.setPaymentMethodLookupCode(newVendor.getPaymentMethodLookupCode());
        }
        if (newVendor.getOperatingUnit() != null) {
            existingVendor.setOperatingUnit(newVendor.getOperatingUnit());
        }
        
        // Save the updated vendor
        vendorRepository.save(existingVendor);
    }
    
    /**
     * Result of the import operation.
     */
    public record ImportResult(
        int importedCount,
        int errorCount,
        String message
    ) {}
}
