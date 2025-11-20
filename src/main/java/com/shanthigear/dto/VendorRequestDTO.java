package com.shanthigear.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shanthigear.model.Vendor;
import com.shanthigear.validation.ValidVendorNumber;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for creating or updating a vendor.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VendorRequestDTO {
    
    @NotBlank(message = "Vendor Number is required")
    @Pattern(regexp = "^[1-9]\\d{4,6}$", 
             message = "Vendor Number must be a 5-6 digit number")
    @ValidVendorNumber
    private String vendorNumber;
    
    @NotBlank(message = "Vendor Name is required")
    @Size(max = 240, message = "Vendor Name must be less than 240 characters")
    private String vendorName;
    
    @NotBlank(message = "Vendor Site is required")
    @Size(max = 15, message = "Vendor Site must be less than 15 characters")
    private String vendorSite;
    
    @NotBlank(message = "Pay Group is required")
    @Size(max = 25, message = "Pay Group must be less than 25 characters")
    private String payGroup;
    
    @Size(max = 240, message = "Address Line 1 must be less than 240 characters")
    private String addressLine1;
    
    @Size(max = 240, message = "Address Line 2 must be less than 240 characters")
    private String addressLine2;
    
    @Size(max = 240, message = "Address Line 3 must be less than 240 characters")
    private String addressLine3;
    
    @Size(max = 60, message = "City must be less than 60 characters")
    private String city;
    
    @Size(max = 60, message = "State must be less than 60 characters")
    private String state;
    
    @Size(max = 60, message = "Pincode must be less than 60 characters")
    private String pincode;
    
    @NotBlank(message = "Bank Account Number is required")
    @Size(max = 60, message = "Bank Account Number must be less than 60 characters")
    private String bankAccountNum;
    
    @NotBlank(message = "Bank Name is required")
    @Size(max = 60, message = "Bank Name must be less than 60 characters")
    private String bankName;
    
    @NotBlank(message = "IFSC Code is required")
    @Size(min = 11, max = 11, message = "IFSC Code must be exactly 11 characters")
    @Pattern(regexp = "^[A-Za-z]{4}0[A-Z0-9a-z]{6}$", message = "Invalid IFSC Code format")
    private String ifscCode;
    
    @Size(max = 60, message = "Branch must be less than 60 characters")
    private String branch;
    
    @Email(message = "Email should be valid")
    @Size(max = 240, message = "Email must be less than 240 characters")
    private String emailAddress;
    
    @Size(max = 30, message = "Vendor Type must be less than 30 characters")
    private String vendorType;
    
    private LocalDate startDateActivity;
    
    @Size(max = 150, message = "Attribute12 must be less than 150 characters")
    private String attribute12;
    
    @Size(max = 150, message = "Attribute13 must be less than 150 characters")
    private String attribute13;
    
    @Size(max = 25, message = "Freight Terms Lookup Code must be less than 25 characters")
    private String freightTermsLookupCode;
    
    @NotBlank(message = "Payment Method Lookup Code is required")
    @Size(max = 25, message = "Payment Method Lookup Code must be less than 25 characters")
    private String paymentMethodLookupCode;
    
    @Size(max = 80, message = "Bank Account Name must be less than 80 characters")
    private String bankAccountName;
    
    @Size(max = 150, message = "Attribute2 must be less than 150 characters")
    private String attribute2;
    
    @Size(max = 150, message = "Attribute3 must be less than 150 characters")
    private String attribute3;
    
    @Size(max = 240, message = "Operating Unit must be less than 240 characters")
    private String operatingUnit;
    
    /**
     * Converts this DTO to a Vendor entity.
     * @return Vendor entity
     */
    public Vendor toEntity() {
        return Vendor.builder()
                .vendorNumber(this.vendorNumber)
                .vendorName(this.vendorName)
                .vendorSite(this.vendorSite)
                .payGroup(this.payGroup)
                .addressLine1(this.addressLine1)
                .addressLine2(this.addressLine2)
                .addressLine3(this.addressLine3)
                .city(this.city)
                .state(this.state)
                .pincode(this.pincode)
                .bankAccountNum(this.bankAccountNum)
                .bankName(this.bankName)
                .ifscCode(this.ifscCode)
                .branch(this.branch)
                .emailAddress(this.emailAddress)
                .vendorType(this.vendorType)
                .startDateActivity(this.startDateActivity)
                .attribute12(this.attribute12)
                .attribute13(this.attribute13)
                .freightTermsLookupCode(this.freightTermsLookupCode)
                .paymentMethodLookupCode(this.paymentMethodLookupCode)
                .bankAccountName(this.bankAccountName)
                .attribute2(this.attribute2)
                .attribute3(this.attribute3)
                .operatingUnit(this.operatingUnit)
                .build();
    }
}
