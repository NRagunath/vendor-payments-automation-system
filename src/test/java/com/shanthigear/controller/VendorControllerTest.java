package com.shanthigear.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanthigear.dto.VendorRequestDTO;
import com.shanthigear.dto.VendorResponseDTO;
import com.shanthigear.exception.ResourceNotFoundException;
import com.shanthigear.exception.VendorAlreadyExistsException;
import com.shanthigear.service.VendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class VendorControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VendorService vendorService;

    @InjectMocks
    private VendorController vendorController;

    private VendorResponseDTO testVendorResponse;
    private VendorRequestDTO testVendorRequest;

    private static final String VENDOR_NUMBER = "10005";
    private static final String VENDOR_NAME = "Test Vendor";
    private static final String VENDOR_EMAIL = "test@example.com";
    private static final String BASE_URL = "/api/vendors";
    private static final String INVALID_VENDOR_NUMBER = "9999"; // Below minimum
    private static final String FUTURE_VENDOR_NUMBER = "200000"; // Above maximum

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(vendorController)
                .setControllerAdvice(new com.shanthigear.exception.GlobalExceptionHandler())
                .apply(springSecurity())
                .build();
        
        testVendorRequest = VendorRequestDTO.builder()
                .vendorNumber(VENDOR_NUMBER)
                .vendorName(VENDOR_NAME)
                .vendorSite("SITE001")
                .payGroup("WIRE")
                .emailAddress(VENDOR_EMAIL)
                .addressLine1("123 Test St")
                .city("Test City")
                .state("Test State")
                .pincode("123456")
                .bankAccountNum("1234567890")
                .bankName("Test Bank")
                .ifscCode("TEST0123456")
                .branch("Test Branch")
                .build();

        testVendorResponse = VendorResponseDTO.builder()
                .vendorNumber(VENDOR_NUMBER)
                .vendorName(VENDOR_NAME)
                .vendorSite("SITE001")
                .payGroup("WIRE")
                .emailAddress(VENDOR_EMAIL)
                .addressLine1("123 Test St")
                .city("Test City")
                .state("Test State")
                .pincode("123456")
                .bankAccountNum("1234567890")
                .bankName("Test Bank")
                .ifscCode("TEST0123456")
                .branch("Test Branch")
                .build();
    }
    @Test
    void createVendor_WhenValidRequest_ShouldReturnCreated() throws Exception {
        // Arrange
        when(vendorService.createVendor(any(VendorRequestDTO.class))).thenReturn(testVendorResponse);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                .with(user("admin").roles("VENDOR_WRITE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testVendorRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Vendor-Number", VENDOR_NUMBER))
                .andExpect(jsonPath("$.vendorNumber").value(VENDOR_NUMBER))
                .andExpect(jsonPath("$.vendorName").value(VENDOR_NAME))
                .andExpect(jsonPath("$.emailAddress").value(VENDOR_EMAIL));

        verify(vendorService, times(1)).createVendor(any(VendorRequestDTO.class));
    }
    
    @Test
    void createVendor_WhenUnauthorized_ShouldReturnForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                .with(anonymous())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testVendorRequest)))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void createVendor_WhenVendorNumberTooLow_ShouldReturnBadRequest() throws Exception {
        // Arrange
        testVendorRequest.setVendorNumber(INVALID_VENDOR_NUMBER);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                .with(user("admin").roles("VENDOR_WRITE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testVendorRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value(containsString("Vendor number must be between 10005 and 144617")));
    }
    
    @Test
    void createVendor_WhenVendorNumberTooHigh_ShouldReturnBadRequest() throws Exception {
        // Arrange
        testVendorRequest.setVendorNumber(FUTURE_VENDOR_NUMBER);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                .with(user("admin").roles("VENDOR_WRITE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testVendorRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value(containsString("Vendor number must be between 10005 and 144617")));
    }
    
    @Test
    void createVendor_WhenInvalidVendorNumberFormat_ShouldReturnBadRequest() throws Exception {
        // Arrange
        testVendorRequest.setVendorNumber("1234A");

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                .with(user("admin").roles("VENDOR_WRITE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testVendorRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value(containsString("Vendor Number must be a 5-6 digit number")));
    }

    @Test
    void createVendor_WhenDuplicateVendor_ShouldReturnConflict() throws Exception {
        // Arrange
        String errorMessage = "Vendor with number " + VENDOR_NUMBER + " already exists";
        when(vendorService.createVendor(any(VendorRequestDTO.class)))
                .thenThrow(new VendorAlreadyExistsException(errorMessage));

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testVendorRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(errorMessage));
    }

    @Test
    void createVendor_WhenInvalidData_ShouldReturnBadRequest() throws Exception {
        // Arrange
        VendorRequestDTO invalidRequest = VendorRequestDTO.builder()
                .vendorNumber("")
                .vendorName("")
                .emailAddress("invalid-email")
                .build();

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void getVendorByVendorNumber_WhenFound_ShouldReturnVendor() throws Exception {
        // Arrange
        when(vendorService.getVendorByVendorNumber(VENDOR_NUMBER)).thenReturn(testVendorResponse);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/{vendorNumber}", VENDOR_NUMBER)
                .with(user("user").roles("VENDOR_READ"))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.vendorNumber").value(VENDOR_NUMBER))
                .andExpect(jsonPath("$.vendorName").value(VENDOR_NAME));
    }
    
    @Test
    void getVendorByVendorNumber_WhenUnauthorized_ShouldReturnForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/{vendorNumber}", VENDOR_NUMBER)
                .with(anonymous())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void getVendorByVendorNumber_WhenNotFound_ShouldReturnNotFound() throws Exception {
        // Arrange
        String errorMessage = "Vendor not found with number: " + VENDOR_NUMBER;
        when(vendorService.getVendorByVendorNumber(VENDOR_NUMBER))
                .thenThrow(new ResourceNotFoundException(errorMessage));

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/{vendorNumber}", VENDOR_NUMBER)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(errorMessage));
    }

    @Test
    void updateVendor_WhenValidRequest_ShouldReturnUpdatedVendor() throws Exception {
        // Arrange
        when(vendorService.updateVendor(eq(VENDOR_NUMBER), any(VendorRequestDTO.class)))
                .thenReturn(testVendorResponse);

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/{vendorNumber}", VENDOR_NUMBER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testVendorRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendorNumber").value(VENDOR_NUMBER))
                .andExpect(jsonPath("$.vendorName").value(VENDOR_NAME));

        verify(vendorService, times(1))
                .updateVendor(eq(VENDOR_NUMBER), any(VendorRequestDTO.class));
    }

    @Test
    void updateVendor_WhenInvalidData_ShouldReturnBadRequest() throws Exception {
        // Arrange
        VendorRequestDTO invalidRequest = VendorRequestDTO.builder()
                .vendorNumber(VENDOR_NUMBER)
                .vendorName("")
                .emailAddress("invalid-email")
                .build();

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/{vendorNumber}", VENDOR_NUMBER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void deleteVendor_WhenVendorExists_ShouldReturnNoContent() throws Exception {
        // Arrange
        doNothing().when(vendorService).deleteVendor(VENDOR_NUMBER);

        // Act & Assert
        mockMvc.perform(delete(BASE_URL + "/{vendorNumber}", VENDOR_NUMBER))
                .andExpect(status().isNoContent())
                .andExpect(header().string("X-Vendor-Number", VENDOR_NUMBER));

        verify(vendorService, times(1)).deleteVendor(VENDOR_NUMBER);
    }

    @Test
    void deleteVendor_WhenVendorNotFound_ShouldReturnNotFound() throws Exception {
        // Arrange
        String errorMessage = "Vendor not found with number: " + VENDOR_NUMBER;
        doThrow(new ResourceNotFoundException(errorMessage))
                .when(vendorService).deleteVendor(VENDOR_NUMBER);

        // Act & Assert
        mockMvc.perform(delete(BASE_URL + "/{vendorNumber}", VENDOR_NUMBER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(errorMessage));
    }

    @Test
    void getAllVendors_ShouldReturnPaginatedVendors() throws Exception {
        // Arrange
        Page<VendorResponseDTO> vendorPage = new PageImpl<>(
                Collections.singletonList(testVendorResponse),
                PageRequest.of(0, 20, Sort.by("vendorName").ascending()),
                1
        );
        when(vendorService.getAllVendors(any(Pageable.class))).thenReturn(vendorPage);

        // Act & Assert
        mockMvc.perform(get(BASE_URL)
                .with(user("admin").roles("VENDOR_READ"))
                .param("page", "0")
                .param("size", "20")
                .param("sort", "vendorName,asc"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(header().string("X-Total-Pages", "1"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].vendorNumber").value(VENDOR_NUMBER))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }
    
    @Test
    void getAllVendors_WithCustomPagination_ShouldReturnCorrectPage() throws Exception {
        // Arrange
        List<VendorResponseDTO> vendors = Arrays.asList(
                testVendorResponse,
                VendorResponseDTO.builder()
                        .vendorNumber("10006")
                        .vendorName("Another Vendor")
                        .emailAddress("another@example.com")
                        .build()
        );
        
        Page<VendorResponseDTO> vendorPage = new PageImpl<>(
                vendors,
                PageRequest.of(0, 1, Sort.by("vendorName").ascending()),
                vendors.size()
        );
        
        when(vendorService.getAllVendors(any(Pageable.class))).thenReturn(vendorPage);

        // Act & Assert
        mockMvc.perform(get(BASE_URL)
                .with(user("admin").roles("VENDOR_READ"))
                .param("page", "0")
                .param("size", "1")
                .param("sort", "vendorName,asc"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(header().string("X-Total-Pages", "2"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].vendorName").value("Another Vendor"))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void searchVendors_WithQuery_ShouldReturnMatchingVendors() throws Exception {
        // Arrange
        String searchQuery = "test";
        Page<VendorResponseDTO> vendorPage = new PageImpl<>(
                Collections.singletonList(testVendorResponse),
                PageRequest.of(0, 20),
                1
        );
        when(vendorService.searchVendors(eq(searchQuery), any(Pageable.class))).thenReturn(vendorPage);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/search")
                .with(user("user").roles("VENDOR_READ"))
                .param("query", searchQuery))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(header().string("X-Total-Pages", "1"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].vendorNumber").value(VENDOR_NUMBER))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }
    
    @Test
    void searchVendors_WithPagination_ShouldReturnPaginatedResults() throws Exception {
        // Arrange
        String searchQuery = "vendor";
        List<VendorResponseDTO> vendors = Arrays.asList(
                testVendorResponse,
                VendorResponseDTO.builder()
                        .vendorNumber("10006")
                        .vendorName("Test Vendor 2")
                        .emailAddress("vendor2@example.com")
                        .build()
        );
        
        Page<VendorResponseDTO> vendorPage = new PageImpl<>(
                Collections.singletonList(vendors.get(1)), // Second page with one item
                PageRequest.of(1, 1, Sort.by("vendorName").ascending()),
                vendors.size()
        );
        
        when(vendorService.searchVendors(eq(searchQuery), any(Pageable.class))).thenReturn(vendorPage);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/search")
                .with(user("user").roles("VENDOR_READ"))
                .param("query", searchQuery)
                .param("page", "1")
                .param("size", "1")
                .param("sort", "vendorName,asc"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(header().string("X-Total-Pages", "2"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].vendorName").value("Test Vendor 2"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void searchVendors_WithShortQuery_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/search")
                .param("query", "a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
