package com.shanthigear.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a system user who can interact with the payment system.
 * This could be an administrator, manager, or other staff member.
 */
/**
 * Represents a system user who can interact with the payment system.
 * This could be an administrator, manager, or other staff member.
 */
@Entity
@Table(name = "APP_USER")
@SequenceGenerator(name = "user_seq", sequenceName = "USER_SEQ", allocationSize = 1)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @Column(name = "ID")
    private Long id;
    
    @Column(name = "IS_DELETED")
    @Builder.Default
    private boolean deleted = false;
    
    @Size(max = 50, message = "Username must be less than 50 characters")
    @Column(name = "USERNAME", nullable = false, unique = true, length = 50)
    private String username;
    
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email must be less than 100 characters")
    @Column(name = "EMAIL", nullable = false, unique = true, length = 100)
    private String email;
    
    @Size(max = 50, message = "First name must be less than 50 characters")
    @Column(name = "FIRST_NAME", length = 50)
    private String firstName;
    
    @Size(max = 20, message = "Phone number must be less than 20 characters")
    @Column(name = "PHONE_NUMBER", length = 20)
    private String phoneNumber;
    
    @Size(max = 50, message = "Last name must be less than 50 characters")
    @Column(name = "LAST_NAME", length = 50)
    private String lastName;
    
    @Size(max = 100, message = "Department must be less than 100 characters")
    @Column(name = "DEPARTMENT", length = 100)
    private String department;
    
    @Column(name = "IS_ACTIVE", nullable = false)
    @Builder.Default
    private boolean active = true;
    
    @Column(name = "LAST_LOGIN")
    private LocalDateTime lastLogin;
    
    @Size(max = 20, message = "Role must be less than 20 characters")
    @Column(name = "ROLE", length = 20)
    private String role;
    
    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
    
    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Gets the user's full name by combining first and last name.
     * @return the full name of the user
     */
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return "";
        } else if (firstName == null) {
            return lastName;
        } else if (lastName == null) {
            return firstName;
        } else {
            return firstName + " " + lastName;
        }
    }
}
