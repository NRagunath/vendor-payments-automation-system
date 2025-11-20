package com.shanthigear.integration;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.shanthigear.model.*;
import com.shanthigear.repository.*;
import com.shanthigear.security.SecurityUtils;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLoggingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @MockBean
    private VendorRepository vendorRepository;

    @MockBean
    private SecurityUtils securityUtils;

    private String testVendorId = "VENDOR-AUDIT-001";
    private String testUserId = "test-user";

    @BeforeEach
    void setUp() {
        // Mock security context
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
        when(securityUtils.getClientIpAddress()).thenReturn(Optional.of("127.0.0.1"));

        // Mock vendor repository
        Vendor vendor = Vendor.builder()
            .vendorNumber(testVendorId)
            .vendorName("Audit Test Vendor")
            .emailAddress("audit.test@example.com")
            .build();
            
        when(vendorRepository.findByVendorNumber(testVendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock audit log repository
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog log = invocation.getArgument(0);
            log.setId(1L);
            return log;
        });
    }

    @Test
    void createVendor_ShouldLogAuditEvent() throws Exception {
        String newVendorId = "VENDOR-AUDIT-NEW";
        String newVendorJson = """
        {
            "vendorId": "%s",
            "name": "New Audit Test Vendor",
            "email": "audit.new@test.com"
        }
        """.formatted(newVendorId);

        // Mock the vendor repository to return empty for the new vendor ID
        when(vendorRepository.findByVendorNumber(newVendorId)).thenReturn(Optional.empty());
        
        // Mock the saved vendor
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/vendors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(newVendorJson))
                .andExpect(status().isCreated());

        // Verify audit log was created
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        
        AuditLog auditLog = auditLogCaptor.getValue();
        assertEquals("VENDOR_CREATED", auditLog.getAction());
        assertEquals("VENDOR", auditLog.getEntityType());
        assertEquals(newVendorId, auditLog.getEntityId());
        assertEquals(testUserId, auditLog.getUserId());
        assertNotNull(auditLog.getTimestamp());
    }

    @Test
    void updateVendor_ShouldLogAuditEvent() throws Exception {
        // Setup existing vendor
        Vendor existingVendor = Vendor.builder()
            .vendorNumber(testVendorId)
            .vendorName("Audit Test Vendor")
            .emailAddress("audit.test@example.com")
            .build();
            
        when(vendorRepository.findByVendorNumber(testVendorId)).thenReturn(Optional.of(existingVendor));
        
        String updateVendorJson = """
        {
            "name": "Updated Audit Vendor",
            "email": "audit.updated@test.com"
        }
        """;

        mockMvc.perform(put("/api/v1/vendors/{vendorNumber}", testVendorId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateVendorJson))
                .andExpect(status().isOk());

        // Verify audit log was created
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        
        AuditLog auditLog = auditLogCaptor.getValue();
        assertEquals("VENDOR_UPDATED", auditLog.getAction());
        assertEquals("VENDOR", auditLog.getEntityType());
        assertEquals(testVendorId, auditLog.getEntityId());
        assertEquals(testUserId, auditLog.getUserId());
        assertNotNull(auditLog.getTimestamp());
        assertTrue(auditLog.getDetails().contains("name=Updated Audit Vendor"));
        assertTrue(auditLog.getDetails().contains("email=audit.updated@test.com"));
    }

    @Test
    void deleteVendor_ShouldLogAuditEvent() throws Exception {
        // Setup existing vendor
        Vendor existingVendor = Vendor.builder()
            .vendorNumber(testVendorId)
            .vendorName("Audit Test Vendor")
            .emailAddress("audit.test@example.com")
            .build();
            
        when(vendorRepository.findByVendorNumber(testVendorId)).thenReturn(Optional.of(existingVendor));
        
        mockMvc.perform(delete("/api/v1/vendors/{vendorNumber}", testVendorId))
                .andExpect(status().isNoContent());

        // Verify audit log was created
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        
        AuditLog auditLog = auditLogCaptor.getValue();
        assertEquals("VENDOR_DELETED", auditLog.getAction());
        assertEquals("VENDOR", auditLog.getEntityType());
        assertEquals(testVendorId, auditLog.getEntityId());
        assertEquals(testUserId, auditLog.getUserId());
        assertNotNull(auditLog.getTimestamp());
    }

    @Test
    void getAuditLogs_ShouldReturnPaginatedResults() throws Exception {
        // Given
        AuditLog log1 = AuditLog.builder()
            .action("VENDOR_CREATED")
            .entityType("VENDOR")
            .entityId(testVendorId)
            .userId(testUserId)
            .userIp("127.0.0.1")
            .build();

        when(auditLogRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(log1)));

        // When/Then
        mockMvc.perform(get("/api/v1/audit-logs")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].action").value("VENDOR_CREATED"))
                .andExpect(jsonPath("$.content[0].entityType").value("VENDOR"))
                .andExpect(jsonPath("$.content[0].entityId").value(testVendorId));
    }

    @Test
    void getAuditLogsByEntity_ShouldReturnFilteredResults() throws Exception {
        // Given
        AuditLog log1 = AuditLog.builder()
            .action("VENDOR_UPDATED")
            .entityType("VENDOR")
            .entityId(testVendorId)
            .userId(testUserId)
            .userIp("127.0.0.1")
            .build();

        when(auditLogRepository.findByEntityTypeAndEntityId(
            eq("VENDOR"), 
            eq(testVendorId), 
            any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(log1)));

        // When/Then
        mockMvc.perform(get("/api/v1/audit-logs/entity/VENDOR/{id}", testVendorId)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].action").value("VENDOR_UPDATED"))
                .andExpect(jsonPath("$.content[0].entityType").value("VENDOR"))
                .andExpect(jsonPath("$.content[0].entityId").value(testVendorId));
    }
}
