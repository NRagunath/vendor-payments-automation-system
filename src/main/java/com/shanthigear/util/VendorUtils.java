package com.shanthigear.util;

import com.shanthigear.model.Vendor;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for vendor-related operations.
 */
@UtilityClass
public class VendorUtils {

    /**
     * Normalizes vendor data by trimming strings and converting to uppercase where appropriate.
     * 
     * @param vendor The vendor to normalize
     * @return The normalized vendor
     */
    public static Vendor normalizeVendor(Vendor vendor) {
        if (vendor == null) {
            return null;
        }
        
        return Vendor.builder()
            .vendorNumber(StringUtils.trimToNull(vendor.getVendorNumber()))
            .vendorName(StringUtils.trimToNull(vendor.getVendorName()))
            .vendorSite(StringUtils.trimToNull(vendor.getVendorSite()))
            .payGroup(StringUtils.trimToNull(vendor.getPayGroup()))
            .emailAddress(vendor.getEmailAddress() != null ? 
                vendor.getEmailAddress().toLowerCase().trim() : null)
            .bankAccountNum(StringUtils.trimToNull(vendor.getBankAccountNum()))
            .bankName(StringUtils.trimToNull(vendor.getBankName()))
            .branch(StringUtils.trimToNull(vendor.getBranch()))
            .ifscCode(vendor.getIfscCode() != null ? 
                vendor.getIfscCode().toUpperCase().replaceAll("\\s+", "") : null)
            .accountNumber(StringUtils.trimToNull(vendor.getAccountNumber()))
            .bankAccountName(StringUtils.trimToNull(vendor.getBankAccountName()))
            .paymentMethodLookupCode(StringUtils.trimToNull(vendor.getPaymentMethodLookupCode()))
            .build();
    }
    
    /**
     * Masks sensitive information in vendor data for logging.
     * 
     * @param vendor The vendor to mask
     * @return A string representation with sensitive data masked
     */
    public static String maskSensitiveData(Vendor vendor) {
        if (vendor == null) {
            return "null";
        }
        
        return String.format(
            "Vendor{vendorNumber='%s', vendorName='%s', vendorSite='%s', emailAddress='%s', " +
            "bankAccountNum='%s', bankName='%s', branch='%s', ifscCode='%s', accountNumber='%s'}",
            maskString(vendor.getVendorNumber()),
            vendor.getVendorName(),
            vendor.getVendorSite(),
            maskEmail(vendor.getEmailAddress()),
            maskAccountNumber(vendor.getBankAccountNum()),
            vendor.getBankName(),
            vendor.getBranch(),
            vendor.getIfscCode(),
            maskAccountNumber(vendor.getAccountNumber())
        );
    }
    
    private static String maskString(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        return "****" + value.substring(Math.max(0, value.length() - 4));
    }
    
    private static String maskAccountNumber(String accountNumber) {
        if (StringUtils.isBlank(accountNumber) || accountNumber.length() < 4) {
            return accountNumber;
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
    
    private static String maskEmail(String email) {
        if (StringUtils.isBlank(email) || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@", 2);
        String username = parts[0];
        String domain = parts[1];
        
        if (username.length() <= 2) {
            return "*@" + domain;
        }
        
        return username.charAt(0) + "*****" + username.charAt(username.length() - 1) + "@" + domain;
    }
    

}
