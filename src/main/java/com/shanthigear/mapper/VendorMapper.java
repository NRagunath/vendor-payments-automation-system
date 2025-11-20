package com.shanthigear.mapper;

import com.shanthigear.dto.VendorRequestDTO;
import com.shanthigear.dto.VendorResponseDTO;
import com.shanthigear.model.Vendor;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;


import java.util.List;

/**
 * Mapper for converting between Vendor entity and DTOs.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
@Component
public interface VendorMapper {

    VendorMapper INSTANCE = Mappers.getMapper(VendorMapper.class);

    /**
     * Converts a Vendor entity to a VendorResponseDTO.
     */
    @Mapping(target = "vendorNumber", source = "vendorNumber")
    @Mapping(target = "vendorName", source = "vendorName")
    @Mapping(target = "vendorSite", source = "vendorSite")
    @Mapping(target = "payGroup", source = "payGroup")
    @Mapping(target = "addressLine1", source = "addressLine1")
    @Mapping(target = "addressLine2", source = "addressLine2")
    @Mapping(target = "addressLine3", source = "addressLine3")
    @Mapping(target = "city", source = "city")
    @Mapping(target = "state", source = "state")
    @Mapping(target = "pincode", source = "pincode")
    @Mapping(target = "accountNumber", source = "accountNumber")
    @Mapping(target = "bankName", source = "bankName")
    @Mapping(target = "ifscCode", source = "ifscCode")
    @Mapping(target = "branch", source = "branch")
    @Mapping(target = "emailAddress", source = "emailAddress")
    @Mapping(target = "creationDate", source = "creationDate", dateFormat = "yyyy-MM-dd")
    @Mapping(target = "vendorType", source = "vendorType")
    @Mapping(target = "startDateActivity", source = "startDateActivity", dateFormat = "yyyy-MM-dd")
    @Mapping(target = "attribute12", source = "attribute12")
    @Mapping(target = "attribute13", source = "attribute13")
    @Mapping(target = "freightTermsLookupCode", source = "freightTermsLookupCode")
    @Mapping(target = "paymentMethodLookupCode", source = "paymentMethodLookupCode")
    @Mapping(target = "bankAccountName", source = "bankAccountName")
    @Mapping(target = "attribute2", source = "attribute2")
    @Mapping(target = "attribute3", source = "attribute3")
    @Mapping(target = "operatingUnit", source = "operatingUnit")
    @Mapping(target = "bankAccountNum", source = "bankAccountNum")
    VendorResponseDTO toResponseDto(Vendor vendor);

    /**
     * Converts a VendorRequestDTO to a Vendor entity.
     */
    @Mapping(target = "creationDate", expression = "java(java.time.LocalDate.now())")
    Vendor toEntity(VendorRequestDTO dto);
    
    /**
     * Updates an existing Vendor entity with values from VendorRequestDTO.
     */
    @Mapping(target = "creationDate", ignore = true)
    void updateVendorFromDto(VendorRequestDTO vendorDTO, @MappingTarget Vendor vendor);
    
    /**
     * Converts a list of Vendor entities to a list of VendorResponseDTOs.
     */
    List<VendorResponseDTO> toResponseDtoList(List<Vendor> vendors);
    
    /**
     * Converts a Vendor entity to a VendorRequestDTO.
     */
    @Mapping(target = "vendorNumber", source = "vendorNumber")
    @Mapping(target = "vendorName", source = "vendorName")
    @Mapping(target = "vendorSite", source = "vendorSite")
    @Mapping(target = "payGroup", source = "payGroup")
    @Mapping(target = "addressLine1", source = "addressLine1")
    @Mapping(target = "addressLine2", source = "addressLine2")
    @Mapping(target = "addressLine3", source = "addressLine3")
    @Mapping(target = "city", source = "city")
    @Mapping(target = "state", source = "state")
    @Mapping(target = "pincode", source = "pincode")
    @Mapping(target = "bankAccountNum", source = "bankAccountNum")
    @Mapping(target = "bankName", source = "bankName")
    @Mapping(target = "ifscCode", source = "ifscCode")
    @Mapping(target = "branch", source = "branch")
    @Mapping(target = "emailAddress", source = "emailAddress")
    @Mapping(target = "vendorType", source = "vendorType")
    @Mapping(target = "startDateActivity", source = "startDateActivity")
    @Mapping(target = "attribute12", source = "attribute12")
    @Mapping(target = "attribute13", source = "attribute13")
    @Mapping(target = "freightTermsLookupCode", source = "freightTermsLookupCode")
    @Mapping(target = "paymentMethodLookupCode", source = "paymentMethodLookupCode")
    @Mapping(target = "bankAccountName", source = "bankAccountName")
    @Mapping(target = "attribute2", source = "attribute2")
    @Mapping(target = "attribute3", source = "attribute3")
    @Mapping(target = "operatingUnit", source = "operatingUnit")
    VendorRequestDTO toRequestDto(Vendor vendor);
}

