package com.shanthigear.service;

import com.shanthigear.dto.VendorRequestDTO;
import com.shanthigear.dto.VendorResponseDTO;
import com.shanthigear.exception.ResourceNotFoundException;
import com.shanthigear.model.Vendor;
import com.shanthigear.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    @Mock
    private VendorRepository vendorRepository;
    
    @Mock
    private ModelMapper modelMapper;
    
    @InjectMocks
    private VendorService vendorService;

    private Vendor testVendor;

    @BeforeEach
    void setUp() {
        testVendor = Vendor.builder()
            .vendorNumber("10005")
            .vendorName("Test Vendor")
            .vendorSite("SITE001")
            .payGroup("WIRE")
            .addressLine1("123 Test St")
            .city("Test City")
            .state("Test State")
            .pincode("123456")
            .bankAccountNum("1234567890")
            .bankName("Test Bank")
            .ifscCode("TEST0123456")
            .branch("Test Branch")
            .emailAddress("vendor@example.com")
            .build();
    }

    @Test
    void getAllVendors_ReturnsAllVendors() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Vendor> vendorPage = new PageImpl<>(List.of(testVendor), pageable, 1);
        VendorResponseDTO responseDTO = VendorResponseDTO.builder()
            .vendorNumber("10005")
            .vendorName("Test Vendor")
            .emailAddress("vendor@example.com")
            .build();
        
        when(vendorRepository.findAll(any(Pageable.class))).thenReturn(vendorPage);
        when(modelMapper.map(any(Vendor.class), eq(VendorResponseDTO.class))).thenReturn(responseDTO);

        // When
        Page<VendorResponseDTO> result = vendorService.getAllVendors(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("10005", result.getContent().get(0).getVendorNumber());
    }

    @Test
    void getVendors_WithPagination_ReturnsPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Vendor> vendorPage = new PageImpl<>(List.of(testVendor), pageable, 1);
        VendorResponseDTO responseDTO = VendorResponseDTO.builder()
            .vendorNumber("10005")
            .vendorName("Test Vendor")
            .emailAddress("vendor@example.com")
            .build();
        
        when(vendorRepository.findAll(pageable)).thenReturn(vendorPage);
        when(modelMapper.map(any(Vendor.class), eq(VendorResponseDTO.class))).thenReturn(responseDTO);

        // When
        Page<VendorResponseDTO> result = vendorService.getAllVendors(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("10005", result.getContent().get(0).getVendorNumber());
    }

    @Test
    void getVendorByVendorNumber_WithExistingVendor_ReturnsVendor() {
        // Given
        String vendorNumber = "10005";
        VendorResponseDTO responseDTO = VendorResponseDTO.builder()
            .vendorNumber(vendorNumber)
            .build();
        
        when(vendorRepository.findById(vendorNumber)).thenReturn(Optional.of(testVendor));
        when(modelMapper.map(any(Vendor.class), eq(VendorResponseDTO.class))).thenReturn(responseDTO);

        // When
        VendorResponseDTO found = vendorService.getVendorByVendorNumber(vendorNumber);

        // Then
        assertNotNull(found);
        assertEquals(vendorNumber, found.getVendorNumber());
    }

    @Test
    void getVendorByVendorNumber_WithNonExistentVendor_ThrowsException() {
        // Given
        String nonExistentVendorNumber = "99999";
        when(vendorRepository.findById(nonExistentVendorNumber)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResourceNotFoundException.class, 
            () -> vendorService.getVendorByVendorNumber(nonExistentVendorNumber));
    }

    @Test
    void createVendor_WithValidData_ReturnsCreatedVendor() {
        // Given
        String vendorNumber = "10005";
        VendorRequestDTO requestDTO = VendorRequestDTO.builder()
            .vendorNumber(vendorNumber)
            .vendorName("New Vendor")
            .vendorSite("SITE001")
            .payGroup("WIRE")
            .emailAddress("new@example.com")
            .build();
            
        Vendor savedVendor = Vendor.builder()
            .vendorNumber(vendorNumber)
            .vendorName("New Vendor")
            .vendorSite("SITE001")
            .payGroup("WIRE")
            .emailAddress("new@example.com")
            .build();
            
        VendorResponseDTO responseDTO = VendorResponseDTO.builder()
            .vendorNumber(vendorNumber)
            .vendorName("New Vendor")
            .emailAddress("new@example.com")
            .build();
            
        when(vendorRepository.existsById(vendorNumber)).thenReturn(false);
        when(vendorRepository.save(any(Vendor.class))).thenReturn(savedVendor);
        when(modelMapper.map(any(VendorRequestDTO.class), eq(Vendor.class))).thenReturn(savedVendor);
        when(modelMapper.map(any(Vendor.class), eq(VendorResponseDTO.class))).thenReturn(responseDTO);

        // When
        VendorResponseDTO result = vendorService.createVendor(requestDTO);

        // Then
        assertNotNull(result);
        assertEquals(vendorNumber, result.getVendorNumber());
        assertEquals("New Vendor", result.getVendorName());
        assertEquals("new@example.com", result.getEmailAddress());
    }

    @Test
    void createVendor_WithExistingVendorId_ThrowsException() {
        // Given
        String existingVendorNumber = "10005";
        VendorRequestDTO requestDTO = VendorRequestDTO.builder()
            .vendorNumber(existingVendorNumber)
            .vendorName("New Vendor")
            .vendorSite("SITE001")
            .payGroup("WIRE")
            .emailAddress("new@example.com")
            .build();
            
        when(vendorRepository.existsById(existingVendorNumber)).thenReturn(true);

        // When/Then
        assertThrows(IllegalArgumentException.class, 
            () -> vendorService.createVendor(requestDTO));
    }

    @Test
    void updateVendor_WithValidData_ReturnsUpdatedVendor() {
        // Given
        String vendorNumber = "10005";
        VendorRequestDTO updateRequest = VendorRequestDTO.builder()
            .vendorName("Updated Vendor")
            .emailAddress("updated@example.com")
            .build();
            
        Vendor updatedVendor = Vendor.builder()
            .vendorNumber(vendorNumber)
            .vendorName("Updated Vendor")
            .emailAddress("updated@example.com")
            .build();
            
        when(vendorRepository.findById(vendorNumber)).thenReturn(Optional.of(testVendor));
        when(vendorRepository.save(any(Vendor.class))).thenReturn(updatedVendor);
        when(modelMapper.map(any(Vendor.class), eq(VendorResponseDTO.class)))
            .thenReturn(VendorResponseDTO.builder()
                .vendorNumber(vendorNumber)
                .vendorName("Updated Vendor")
                .emailAddress("updated@example.com")
                .build());

        // When
        VendorResponseDTO updated = vendorService.updateVendor(vendorNumber, updateRequest);

        // Then
        assertNotNull(updated);
        assertEquals("Updated Vendor", updated.getVendorName());
        assertEquals("updated@example.com", updated.getEmailAddress());
    }

    @Test
    void deleteVendor_WithExistingVendor_DeletesVendor() {
        // Given
        String vendorNumber = "10005";
        when(vendorRepository.existsById(vendorNumber)).thenReturn(true);
        doNothing().when(vendorRepository).deleteById(vendorNumber);

        // When
        vendorService.deleteVendor(vendorNumber);

        // Then
        verify(vendorRepository, times(1)).deleteById(vendorNumber);
    }

    @Test
    void deleteVendor_WithNonExistentVendor_ThrowsException() {
        // Given
        String nonExistentVendorNumber = "99999";
        when(vendorRepository.existsById(nonExistentVendorNumber)).thenReturn(false);

        // When/Then
        assertThrows(ResourceNotFoundException.class, 
            () -> vendorService.deleteVendor(nonExistentVendorNumber));
        
        verify(vendorRepository, never()).deleteById(anyString());
    }

    @Test
    void searchVendors_WithQuery_ReturnsMatchingVendors() {
        // Given
        String query = "test";
        Pageable pageable = PageRequest.of(0, 10);
        Page<Vendor> vendorPage = new PageImpl<>(List.of(testVendor), pageable, 1);
        
        when(vendorRepository.findByNameContainingIgnoreCase(eq(query), any(Pageable.class))).thenReturn(vendorPage);
        when(vendorRepository.findByEmailContainingIgnoreCase(eq(query), any(Pageable.class))).thenReturn(Page.empty());
        when(modelMapper.map(any(Vendor.class), eq(VendorResponseDTO.class)))
            .thenReturn(VendorResponseDTO.builder().vendorNumber("10005").build());

        // When
        Page<VendorResponseDTO> result = vendorService.searchVendors(query, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(vendorRepository, times(1)).findByNameContainingIgnoreCase(eq(query), any(Pageable.class));
        verify(vendorRepository, times(1)).findByEmailContainingIgnoreCase(eq(query), any(Pageable.class));
    }
}
