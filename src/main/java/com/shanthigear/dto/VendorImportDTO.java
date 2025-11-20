package com.shanthigear.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;

/**
 * DTO for importing vendor data from Excel
 * Maps to the vendor master Excel file format
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorImportDTO {
    // Required fields
    private String vendorNumber;
    private String vendorName;
    private String vendorSite;
    private String payGroup;
    
    // Address fields
    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private String city;
    private String state;
    private String pincode;
    
    // Bank details
    private String bankAccountNum;
    private String bankName;
    private String ifscCode;
    private String branch;
    
    // Contact information
    private String emailAddress;
    private String creationDate;
    private String vendorType;
    private String startDateActivity;
    
    // Additional attributes
    private String attribute12;
    private String attribute13;
    private String freightTermsLookupCode;
    private String paymentMethodLookupCode;
    private String bankAccountName;
    private String attribute2;
    private String attribute3;
    private String operatingUnit;
    
    /**
     * Validates the vendor import data
     * @return List of validation error messages, empty if valid
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        
        // Required fields validation
        if (StringUtils.isBlank(vendorNumber)) errors.add("Vendor Number is required");
        if (StringUtils.isBlank(vendorName)) errors.add("Vendor Name is required");
        if (StringUtils.isBlank(vendorSite)) errors.add("Vendor Site is required");
        if (StringUtils.isBlank(payGroup)) errors.add("Pay Group is required");
        if (StringUtils.isBlank(bankAccountNum)) errors.add("Bank Account Number is required");
        if (StringUtils.isBlank(bankName)) errors.add("Bank Name is required");
        if (StringUtils.isBlank(ifscCode)) errors.add("IFSC Code is required");
        if (StringUtils.isBlank(paymentMethodLookupCode)) errors.add("Payment Method Lookup Code is required");
        
        // Format validation
        if (StringUtils.isNotBlank(ifscCode) && !ifscCode.matches("^[A-Za-z]{4}0[A-Z0-9a-z]{6}$")) {
            errors.add("Invalid IFSC code format. Must be 11 characters (e.g., HDFC0001234)");
        }
        
        if (StringUtils.isNotBlank(emailAddress) && !emailAddress.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errors.add("Invalid email format: " + emailAddress);
        }
        
        // Date format validation
        if (StringUtils.isNotBlank(creationDate)) {
            try {
                LocalDate.parse(creationDate, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                errors.add("Invalid Creation Date format. Use YYYY-MM-DD");
            }
        }
        
        if (StringUtils.isNotBlank(startDateActivity)) {
            try {
                LocalDate.parse(startDateActivity, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                errors.add("Invalid Start Date Activity format. Use YYYY-MM-DD");
            }
        }
        
        // Field length validations
        if (StringUtils.isNotBlank(vendorNumber) && vendorNumber.length() > 30) {
            errors.add("Vendor Number must be 30 characters or less");
        }
        if (StringUtils.isNotBlank(vendorName) && vendorName.length() > 240) {
            errors.add("Vendor Name must be 240 characters or less");
        }
        if (StringUtils.isNotBlank(vendorSite) && vendorSite.length() > 15) {
            errors.add("Vendor Site must be 15 characters or less");
        }
        if (StringUtils.isNotBlank(payGroup) && payGroup.length() > 25) {
            errors.add("Pay Group must be 25 characters or less");
        }
        if (StringUtils.isNotBlank(bankAccountNum) && bankAccountNum.length() > 60) {
            errors.add("Bank Account Number must be 60 characters or less");
        }
        if (StringUtils.isNotBlank(ifscCode) && ifscCode.length() != 11) {
            errors.add("IFSC Code must be exactly 11 characters");
        }
        
        return errors;
    }
}
