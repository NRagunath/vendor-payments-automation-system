package com.shanthigear.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Represents a vendor in the system who receives payments.
 * Follows the Oracle Vendor Master structure for H2H payments.
 */
@Entity
@Table(name = "VENDOR_MASTER")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vendor {
    
    @Id
    @Column(name = "VENDOR_NUMBER", length = 6, nullable = false, columnDefinition = "VARCHAR2(6) CHECK (REGEXP_LIKE(VENDOR_NUMBER, '^[1-9]\\d{4,5}$') AND TO_NUMBER(VENDOR_NUMBER) BETWEEN 10005 AND 144617)")
    @Pattern(regexp = "^[1-9]\\d{4,5}$", message = "Vendor number must be a 5-6 digit number between 10005 and 144617")
    private String vendorNumber;
    
    @NotBlank(message = "Vendor Name is required")
    @Size(max = 240, message = "Vendor Name must be less than 240 characters")
    @Column(name = "VENDOR_NAME", length = 240, nullable = false)
    private String vendorName;
    
    @NotBlank(message = "Vendor Site is required")
    @Size(max = 15, message = "Vendor Site must be less than 15 characters")
    @Column(name = "VENDOR_SITE", length = 15, nullable = false)
    private String vendorSite;
    
    @NotBlank(message = "Pay Group is required")
    @Size(max = 25, message = "Pay Group must be less than 25 characters")
    @Column(name = "PAY_GROUP", length = 25, nullable = false)
    private String payGroup;
    
    @Size(max = 240, message = "Address Line 1 must be less than 240 characters")
    @Column(name = "ADDRESS_LINE1", length = 240)
    private String addressLine1;
    
    @Size(max = 240, message = "Address Line 2 must be less than 240 characters")
    @Column(name = "ADDRESS_LINE2", length = 240)
    private String addressLine2;
    
    @Size(max = 240, message = "Address Line 3 must be less than 240 characters")
    @Column(name = "ADDRESS_LINE3", length = 240)
    private String addressLine3;
    
    @Size(max = 60, message = "City must be less than 60 characters")
    @Column(name = "CITY", length = 60)
    private String city;
    
    @Size(max = 60, message = "State must be less than 60 characters")
    @Column(name = "STATE", length = 60)
    private String state;
    
    @Size(max = 60, message = "Pincode must be less than 60 characters")
    @Column(name = "PINCODE", length = 60)
    private String pincode;
    
    @NotBlank(message = "Account Number is required")
    @Size(max = 60, message = "Account Number must be less than 60 characters")
    @Column(name = "ACCOUNT_NUMBER", length = 60, nullable = false)
    private String accountNumber;
    
    @NotBlank(message = "Bank Name is required")
    @Size(max = 60, message = "Bank Name must be less than 60 characters")
    @Column(name = "BANK_NAME", length = 60, nullable = false)
    private String bankName;
    
    @NotBlank(message = "IFSC Code is required")
    @Size(max = 11, message = "IFSC Code must be 11 characters")
    @Column(name = "IFSC_CODE", length = 11, nullable = false)
    private String ifscCode;
    
    @Size(max = 60, message = "Branch must be less than 60 characters")
    @Column(name = "BRANCH", length = 60)
    private String branch;
    
    @Email(message = "Email should be valid")
    @Size(max = 240, message = "Email must be less than 240 characters")
    @Column(name = "EMAIL_ADDRESS", length = 240)
    private String emailAddress;
    
    @Column(name = "CREATION_DATE")
    private LocalDate creationDate;
    
    @Size(max = 30, message = "Vendor Type must be less than 30 characters")
    @Column(name = "VENDOR_TYPE", length = 30)
    private String vendorType;
    
    @Column(name = "START_DATE_ACTIVITY")
    private LocalDate startDateActivity;
    
    @Size(max = 150, message = "Attribute12 must be less than 150 characters")
    @Column(name = "ATTRIBUTE12", length = 150)
    private String attribute12;
    
    @Size(max = 150, message = "Attribute13 must be less than 150 characters")
    @Column(name = "ATTRIBUTE13", length = 150)
    private String attribute13;
    
    @Size(max = 25, message = "Freight Terms Lookup Code must be less than 25 characters")
    @Column(name = "FREIGHT_TERMS_LOOKUP_CODE", length = 25)
    private String freightTermsLookupCode;
    
    @NotBlank(message = "Payment Method Lookup Code is required")
    @Size(max = 25, message = "Payment Method Lookup Code must be less than 25 characters")
    @Column(name = "PAYMENT_METHOD_LOOKUP_CODE", length = 25, nullable = false)
    private String paymentMethodLookupCode;
    
    @Size(max = 80, message = "Bank Account Name must be less than 80 characters")
    @Column(name = "BANK_ACCOUNT_NAME", length = 80)
    private String bankAccountName;
    
    @Size(max = 150, message = "Attribute2 must be less than 150 characters")
    @Column(name = "ATTRIBUTE2", length = 150)
    private String attribute2;
    
    @Size(max = 150, message = "Attribute3 must be less than 150 characters")
    @Column(name = "ATTRIBUTE3", length = 150)
    private String attribute3;
    
    @Size(max = 240, message = "Operating Unit must be less than 240 characters")
    @Column(name = "OPERATING_UNIT", length = 240)
    private String operatingUnit;
    
    @NotBlank(message = "Bank Account Number is required")
    @Size(max = 60, message = "Bank Account Number must be less than 60 characters")
    @Column(name = "BANK_ACCOUNT_NUM", length = 60, nullable = false)
    private String bankAccountNum;
    
    @Override
    public String toString() {
        return "Vendor{" +
                "vendorNumber='" + vendorNumber + '\'' +
                ", vendorName='" + vendorName + '\'' +
                ", vendorSite='" + vendorSite + '\'' +
                ", payGroup='" + payGroup + '\'' +
                ", emailAddress='" + emailAddress + '\'' +
                ", bankAccountNum='" + bankAccountNum + '\'' +
                ", bankName='" + bankName + '\'' +
                ", ifscCode='" + ifscCode + '\'' +
                '}';
    }
}
