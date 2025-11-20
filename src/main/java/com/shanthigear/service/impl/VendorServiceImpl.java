package com.shanthigear.service.impl;

import com.shanthigear.dto.VendorRequestDTO;
import com.shanthigear.dto.VendorResponseDTO;
import com.shanthigear.exception.ResourceNotFoundException;
import com.shanthigear.exception.VendorAlreadyExistsException;
import com.shanthigear.exception.InvalidVendorDataException;
import com.shanthigear.model.Vendor;
import com.shanthigear.repository.VendorRepository;
import com.shanthigear.service.VendorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Implementation of the VendorService interface.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepository;
    private final ModelMapper modelMapper;
    
    private static final String VENDOR_NUMBER_PATTERN = "^[A-Za-z0-9-]+$";
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";
    private static final String IFSC_CODE_PATTERN = "^[A-Z]{4}0[A-Z0-9]{6}$";
    private static final String PINCODE_PATTERN = "^[1-9][0-9]{5}$";

    @Override
    @Transactional
    public VendorResponseDTO createVendor(VendorRequestDTO vendorRequestDTO) 
            throws VendorAlreadyExistsException, InvalidVendorDataException {
        
        // Validate vendor request
        List<String> validationErrors = validateVendorRequest(vendorRequestDTO);
        if (!validationErrors.isEmpty()) {
            throw new InvalidVendorDataException("Invalid vendor data", validationErrors);
        }
        
        // Check if vendor already exists
        String vendorNumber = vendorRequestDTO.getVendorNumber();
        if (vendorRepository.existsByVendorNumber(vendorNumber)) {
            throw new VendorAlreadyExistsException("Vendor with number " + vendorNumber + " already exists");
        }
        
        // Check if email is already in use
        if (StringUtils.isNotBlank(vendorRequestDTO.getEmailAddress())) {
            List<Vendor> existingVendors = vendorRepository.findByEmailAddress(vendorRequestDTO.getEmailAddress());
            if (existingVendors != null && !existingVendors.isEmpty()) {
                throw new InvalidVendorDataException("Email address already in use");
            }
        }
        
        // Map DTO to entity using ModelMapper
        Vendor vendor = modelMapper.map(vendorRequestDTO, Vendor.class);
        
        // Save the vendor
        Vendor savedVendor = vendorRepository.save(vendor);
        log.info("Created new vendor with number: {}", savedVendor.getVendorNumber());
        
        // Map entity to DTO and return
        return modelMapper.map(savedVendor, VendorResponseDTO.class);
    }
    
    private List<String> validateVendorRequest(VendorRequestDTO request) {
        List<String> errors = new ArrayList<>();
        
        // Validate vendor number
        if (StringUtils.isBlank(request.getVendorNumber())) {
            errors.add("Vendor number is required");
        } else if (!Pattern.matches(VENDOR_NUMBER_PATTERN, request.getVendorNumber())) {
            errors.add("Vendor number can only contain alphanumeric characters and hyphens");
        }
        
        // Validate vendor name
        if (StringUtils.isBlank(request.getVendorName())) {
            errors.add("Vendor name is required");
        } else if (request.getVendorName().length() > 240) {
            errors.add("Vendor name must be less than 240 characters");
        }
        
        // Validate email
        if (StringUtils.isNotBlank(request.getEmailAddress()) && 
            !Pattern.matches(EMAIL_PATTERN, request.getEmailAddress())) {
            errors.add("Invalid email format");
        }
        
        // Validate IFSC code if provided
        if (StringUtils.isNotBlank(request.getIfscCode()) && 
            !Pattern.matches(IFSC_CODE_PATTERN, request.getIfscCode())) {
            errors.add("Invalid IFSC code format");
        }
        
        // Validate pincode if provided
        if (StringUtils.isNotBlank(request.getPincode()) && 
            !Pattern.matches(PINCODE_PATTERN, request.getPincode())) {
            errors.add("Invalid pincode format");
        }
        
        // Validate bank account details if any bank field is provided
        if (StringUtils.isNotBlank(request.getBankAccountNum()) || 
            StringUtils.isNotBlank(request.getBankName()) || 
            StringUtils.isNotBlank(request.getIfscCode())) {
            
            if (StringUtils.isBlank(request.getBankAccountNum())) {
                errors.add("Bank account number is required when providing bank details");
            }
            if (StringUtils.isBlank(request.getBankName())) {
                errors.add("Bank name is required when providing bank details");
            }
            if (StringUtils.isBlank(request.getIfscCode())) {
                errors.add("IFSC code is required when providing bank details");
            }
        }
        
        return errors;
    }
    
    @Override
    @Transactional
    public VendorResponseDTO updateVendor(String vendorNumber, VendorRequestDTO vendorRequestDTO) 
            throws ResourceNotFoundException, InvalidVendorDataException {
        
        // Find existing vendor
        Vendor existingVendor = vendorRepository.findByVendorNumber(vendorNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with number: " + vendorNumber));
        
        // Validate vendor request
        List<String> validationErrors = validateVendorRequest(vendorRequestDTO);
        if (!validationErrors.isEmpty()) {
            throw new InvalidVendorDataException("Invalid vendor data", validationErrors);
        }
        
        // Check if email is being changed and already in use by another vendor
        if (StringUtils.isNotBlank(vendorRequestDTO.getEmailAddress()) && 
            !StringUtils.equalsIgnoreCase(existingVendor.getEmailAddress(), vendorRequestDTO.getEmailAddress()) &&
            vendorRepository.findByEmailAddress(vendorRequestDTO.getEmailAddress())
                .stream()
                .anyMatch(v -> !v.getVendorNumber().equals(vendorNumber))) {
            throw new InvalidVendorDataException("Email address already in use by another vendor");
        }
        
        // Update fields from DTO using ModelMapper
        modelMapper.map(vendorRequestDTO, existingVendor);
        
        // Save updated vendor
        Vendor updatedVendor = vendorRepository.save(existingVendor);
        log.info("Updated vendor with number: {}", vendorNumber);
        
        return modelMapper.map(updatedVendor, VendorResponseDTO.class);
    }
    
    @Override
    public VendorResponseDTO getVendorByVendorNumber(String vendorNumber) {
        Vendor vendor = vendorRepository.findByVendorNumber(vendorNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with number: " + vendorNumber));
        return modelMapper.map(vendor, VendorResponseDTO.class);
    }
    
    @Override
    public Page<VendorResponseDTO> getAllVendors(Pageable pageable) {
        return vendorRepository.findAll(pageable)
                .map(vendor -> modelMapper.map(vendor, VendorResponseDTO.class));
    }
    
    @Override
    public Page<VendorResponseDTO> searchVendors(String query, Pageable pageable) {
        // Search in vendor name or email (case-insensitive)
        Page<Vendor> nameResults = vendorRepository.findByNameContainingIgnoreCase(query, pageable);
        if (nameResults != null && !nameResults.isEmpty()) {
            return nameResults.map(vendor -> modelMapper.map(vendor, VendorResponseDTO.class));
        }
        
        // If no results from name search, try email search
        Page<Vendor> emailResults = vendorRepository.findByEmailContainingIgnoreCase(query, pageable);
        return emailResults != null 
            ? emailResults.map(vendor -> modelMapper.map(vendor, VendorResponseDTO.class)) 
            : Page.empty();
    }
    
    @Override
    @Transactional
    public void deleteVendor(String vendorNumber) {
        if (!vendorRepository.existsByVendorNumber(vendorNumber)) {
            throw new ResourceNotFoundException("Vendor not found with number: " + vendorNumber);
        }
        vendorRepository.deleteByVendorNumber(vendorNumber);
        log.info("Deleted vendor with number: {}", vendorNumber);
    }
    
    @Override
    public Optional<Vendor> findByVendorNumber(String vendorNumber) {
        return vendorRepository.findByVendorNumber(vendorNumber);
    }
    
    @Override
    public boolean isEligibleForH2H(String vendorNumber) {
        Optional<Vendor> vendorOpt = vendorRepository.findByVendorNumber(vendorNumber);
        if (vendorOpt.isEmpty()) {
            return false;
        }
        Vendor vendor = vendorOpt.get();
        // Check if vendor has all required fields for H2H
        return StringUtils.isNotBlank(vendor.getBankAccountNum()) &&
               StringUtils.isNotBlank(vendor.getIfscCode()) &&
               StringUtils.isNotBlank(vendor.getBankName()) &&
               StringUtils.isNotBlank(vendor.getEmailAddress()) &&
               StringUtils.isNotBlank(vendor.getPayGroup());
    }
    
    @Override
    public boolean existsByVendorNumber(String vendorNumber) {
        return vendorRepository.existsByVendorNumber(vendorNumber);
    }
    
    // Helper methods for mapping between entity and DTO
}
