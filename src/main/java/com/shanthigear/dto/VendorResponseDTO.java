package com.shanthigear.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.shanthigear.model.Vendor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for vendor responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendorResponseDTO {
    
    private String vendorNumber;
    private String vendorName;
    private String vendorSite;
    private String payGroup;
    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private String city;
    private String state;
    private String pincode;
    private String accountNumber;
    private String bankName;
    private String ifscCode;
    private String branch;
    private String emailAddress;
    private String vendorType;
    private LocalDate startDateActivity;
    private String attribute12;
    private String attribute13;
    private String freightTermsLookupCode;
    private String paymentMethodLookupCode;
    private String bankAccountName;
    private String attribute2;
    private String attribute3;
    private String operatingUnit;
    private String bankAccountNum;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate creationDate;
    
    /**
     * Creates a VendorResponseDTO from a Vendor entity.
     * @param vendor the vendor entity
     * @return VendorResponseDTO
     */
    public static VendorResponseDTO fromEntity(Vendor vendor) {
        if (vendor == null) {
            return null;
        }
        
        return VendorResponseDTO.builder()
                .vendorNumber(vendor.getVendorNumber())
                .vendorName(vendor.getVendorName())
                .vendorSite(vendor.getVendorSite())
                .payGroup(vendor.getPayGroup())
                .addressLine1(vendor.getAddressLine1())
                .addressLine2(vendor.getAddressLine2())
                .addressLine3(vendor.getAddressLine3())
                .city(vendor.getCity())
                .state(vendor.getState())
                .pincode(vendor.getPincode())
                .accountNumber(vendor.getAccountNumber())
                .bankName(vendor.getBankName())
                .ifscCode(vendor.getIfscCode())
                .branch(vendor.getBranch())
                .emailAddress(vendor.getEmailAddress())
                .vendorType(vendor.getVendorType())
                .startDateActivity(vendor.getStartDateActivity())
                .attribute12(vendor.getAttribute12())
                .attribute13(vendor.getAttribute13())
                .freightTermsLookupCode(vendor.getFreightTermsLookupCode())
                .paymentMethodLookupCode(vendor.getPaymentMethodLookupCode())
                .bankAccountName(vendor.getBankAccountName())
                .attribute2(vendor.getAttribute2())
                .attribute3(vendor.getAttribute3())
                .operatingUnit(vendor.getOperatingUnit())
                .bankAccountNum(vendor.getBankAccountNum())
                .creationDate(vendor.getCreationDate())
                .build();
    }
}
