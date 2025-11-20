package com.shanthigear.service;

import com.shanthigear.dto.VendorRequestDTO;
import com.shanthigear.dto.VendorResponseDTO;
import com.shanthigear.exception.ResourceNotFoundException;
import com.shanthigear.exception.VendorAlreadyExistsException;
import com.shanthigear.exception.InvalidVendorDataException;
import com.shanthigear.model.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Service interface for managing vendors.
 */
public interface VendorService {
    
    /**
     * Create a new vendor.
     * @param vendorRequestDTO the vendor data to create
     * @return the created vendor DTO
     * @throws VendorAlreadyExistsException if a vendor with the same vendor number already exists
     * @throws InvalidVendorDataException if the vendor data is invalid
     */
    VendorResponseDTO createVendor(VendorRequestDTO vendorRequestDTO) 
        throws VendorAlreadyExistsException, InvalidVendorDataException;
    
    /**
     * Update an existing vendor.
     * @param vendorNumber the vendor number of the vendor to update
     * @param vendorRequestDTO the updated vendor data
     * @return the updated vendor DTO
     * @throws ResourceNotFoundException if the vendor is not found
     * @throws InvalidVendorDataException if the vendor data is invalid
     */
    VendorResponseDTO updateVendor(String vendorNumber, VendorRequestDTO vendorRequestDTO) 
        throws ResourceNotFoundException, InvalidVendorDataException;
    
    /**
     * Get a vendor by vendor number.
     * @param vendorNumber the vendor number to retrieve
     * @return the vendor DTO
     * @throws ResourceNotFoundException if the vendor is not found
     */
    VendorResponseDTO getVendorByVendorNumber(String vendorNumber) 
        throws ResourceNotFoundException;
    
    /**
     * Get all vendors with pagination.
     * @param pageable pagination information
     * @return page of vendor DTOs
     */
    Page<VendorResponseDTO> getAllVendors(Pageable pageable);
    
    /**
     * Search vendors by name or email with pagination.
     * @param query the search query
     * @param pageable pagination information
     * @return page of vendor DTOs matching the search criteria
     */
    Page<VendorResponseDTO> searchVendors(String query, Pageable pageable);
    
    /**
     * Delete a vendor by vendor number.
     * @param vendorNumber the vendor number of the vendor to delete
     */
    void deleteVendor(String vendorNumber);
    
    /**
     * Find a vendor by vendor number.
     * @param vendorNumber the vendor number to search for
     * @return an Optional containing the vendor if found, empty otherwise
     */
    Optional<Vendor> findByVendorNumber(String vendorNumber);
    
    /**
     * Check if a vendor is eligible for Host to Host (H2H) payments.
     * A vendor is eligible if they have all required bank details.
     * @param vendorNumber the vendor number to check
     * @return true if the vendor is eligible for H2H payments, false otherwise
     */
    boolean isEligibleForH2H(String vendorNumber);
    
    /**
     * Check if a vendor exists with the given vendor number.
     * @param vendorNumber the vendor number to check
     * @return true if a vendor with the given number exists, false otherwise
     */
    boolean existsByVendorNumber(String vendorNumber);
}
