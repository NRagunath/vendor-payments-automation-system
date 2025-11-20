package com.shanthigear.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "AUDIT_LOGS")
@SequenceGenerator(name = "audit_logs_seq", sequenceName = "AUDIT_LOGS_SEQ", allocationSize = 1)
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_logs_seq")
    @Column(name = "ID")
    private Long id;
    
    @Column(name = "ACTION", nullable = false, length = 50)
    private String action;
    
    @Column(name = "ENTITY_TYPE", nullable = false, length = 50)
    private String entityType;
    
    @Column(name = "ENTITY_ID", length = 50)
    private String entityId;
    
    @Column(name = "USER_ID", length = 50)
    private String userId;
    
    @Column(name = "USER_IP", length = 50)
    private String userIp;
    
    @Lob
    @Column(name = "DETAILS")
    private String details;
    
    @Builder.Default
    @Column(name = "TIMESTAMP", nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
    
    public AuditLog(String action, String entityType, String entityId, String userId, String userIp) {
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.userId = userId;
        this.userIp = userIp;
    }
}
