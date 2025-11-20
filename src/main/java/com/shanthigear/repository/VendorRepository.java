package com.shanthigear.repository;

import com.shanthigear.model.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Vendor entities.
 */
@Repository
public interface VendorRepository extends JpaRepository<Vendor, String> {
    
    /**
     * Checks if a vendor exists with the given vendor number.
     *
     * @param vendorNumber the vendor number to check
     * @return true if a vendor with the given number exists, false otherwise
     */
    boolean existsByVendorNumber(String vendorNumber);
    
    /**
     * Find a vendor by its vendor number.
     *
     * @param vendorNumber the vendor number to search for
     * @return an Optional containing the vendor if found, or empty if not
     */
    Optional<Vendor> findByVendorNumber(String vendorNumber);
    
    /**
     * Find vendors by bank account number.
     * @param bankAccountNum the bank account number to search for
     * @return list of vendors with the given bank account number
     */
    List<Vendor> findByBankAccountNum(String bankAccountNum);
    
    /**
     * Find vendors by email address.
     * @param emailAddress the email address to search for
     * @return list of vendors with the given email address
     */
    List<Vendor> findByEmailAddress(String emailAddress);
    
    // H2H eligibility is determined by checking for required fields in the service layer
    // rather than a database flag
    
    /**
     * Delete vendor by vendor ID.
     * @param vendorId the vendor ID to delete
     * @return number of vendors deleted
     * @deprecated Use deleteByVendorNumber instead
     */
    @Deprecated
    @Modifying
    @Query("DELETE FROM Vendor v WHERE v.vendorId = :vendorId")
    int deleteByVendorId(@Param("vendorId") String vendorId);
    
    /**
     * Delete vendor by vendor number.
     * @param vendorNumber the vendor number to delete
     * @return number of vendors deleted
     */
    @Modifying
    @Query("DELETE FROM Vendor v WHERE v.vendorNumber = :vendorNumber")
    int deleteByVendorNumber(@Param("vendorNumber") String vendorNumber);
    
    /**
     * Check if a vendor with the given email exists, excluding the vendor with the specified ID.
     * @param email the email to check
     * @param id the ID to exclude
     * @return true if another vendor with the given email exists, false otherwise
     */
    boolean existsByEmailAndIdNot(String email, Long id);
    
    /**
     * Find vendors by name containing the given string (case-insensitive).
     * @param name the name to search for
     * @param pageable pagination information
     * @return page of vendors matching the search criteria
     */
    Page<Vendor> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    /**
     * Find vendors by email containing the given string (case-insensitive).
     * @param email the email to search for
     * @param pageable pagination information
     * @return page of vendors matching the search criteria
     */
    Page<Vendor> findByEmailContainingIgnoreCase(String email, Pageable pageable);
    

    /**
     * Check if a vendor with the given vendor ID exists, excluding the vendor with the given ID.
     * @param vendorId the vendor ID to check
     * @param id the ID of the vendor to exclude
     * @return true if another vendor with the given vendor ID exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM Vendor v WHERE v.vendorId = :vendorId AND v.id <> :id")
    boolean existsByVendorIdAndIdNot(@Param("vendorId") String vendorId, @Param("id") Long id);
}
