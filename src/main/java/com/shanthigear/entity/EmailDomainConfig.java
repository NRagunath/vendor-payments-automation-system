package com.shanthigear.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_domain_config")
@Data
public class EmailDomainConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String domain;
    
    @Column(nullable = false)
    private String host;
    
    @Column(nullable = false)
    private int port;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    private String protocol = "smtp";
    private boolean auth = true;
    private boolean starttlsEnable = true;
    private boolean sslTrust = true;
    private int connectionTimeout = 5000;
    private int timeout = 5000;
    private int writeTimeout = 5000;
    private boolean active = true;
    
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
