package com.shanthigear.validation;

import com.shanthigear.model.Vendor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates vendor data.
 */
@Component
public class VendorValidator {
    
    private static final Pattern IFSC_PATTERN = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^[0-9]{9,18}$");
    
    /**
     * Validates a vendor object.
     * 
     * @param vendor The vendor to validate
     * @return List of validation errors, empty if valid
     */
    public List<String> validate(Vendor vendor) {
        List<String> errors = new ArrayList<>();
        
        if (vendor == null) {
            errors.add("Vendor cannot be null");
            return errors;
        }
        
        // Validate required fields
        if (StringUtils.isBlank(vendor.getVendorNumber())) {
            errors.add("Vendor number is required");
        } else if (vendor.getVendorNumber().length() > 50) {
            errors.add("Vendor number must be less than 50 characters");
        }
        
        if (StringUtils.isBlank(vendor.getVendorName())) {
            errors.add("Vendor name is required");
        } else if (vendor.getVendorName().length() > 100) {
            errors.add("Vendor name must be less than 100 characters");
        }
        
        // Validate email
        if (StringUtils.isNotBlank(vendor.getEmailAddress()) && 
            !EMAIL_PATTERN.matcher(vendor.getEmailAddress()).matches()) {
            errors.add("Invalid email format");
        }
        
        // Validate bank account number (required for H2H payments)
        if (StringUtils.isBlank(vendor.getBankAccountNum())) {
            errors.add("Bank account number is required");
        } else if (!ACCOUNT_NUMBER_PATTERN.matcher(vendor.getBankAccountNum()).matches()) {
            errors.add("Bank account number must be 9-18 digits");
        }
        
        // Validate IFSC code (required for H2H payments)
        if (StringUtils.isBlank(vendor.getIfscCode())) {
            errors.add("IFSC code is required");
        }
        
        // Validate IFSC code
        if (StringUtils.isNotBlank(vendor.getIfscCode()) && 
            !IFSC_PATTERN.matcher(vendor.getIfscCode()).matches()) {
            errors.add("Invalid IFSC code format");
        }
        
        return errors;
    }
    
    /**
     * Validates a vendor object and throws an exception if invalid.
     * 
     * @param vendor The vendor to validate
     * @throws com.shanthigear.exception.ValidationException if validation fails
     */
    public void validateAndThrow(Vendor vendor) {
        List<String> errors = validate(vendor);
        if (!errors.isEmpty()) {
            throw new com.shanthigear.exception.ValidationException("Vendor validation failed", errors);
        }
    }
}
