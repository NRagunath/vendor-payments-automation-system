package com.shanthigear.dto;

import com.shanthigear.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data Transfer Object for user registration.
 * Contains validation annotations for input validation.
 */
@Data
public class UserRegistrationDto {
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @ValidPassword(
        minLength = 8,
        requireDigit = true,
        requireLowercase = true,
        requireUppercase = true,
        requireSpecialChar = true,
        maxRepeatedChars = 2
    )
    private String password;
    
    // Additional fields can be added as needed
    private String firstName;
    private String lastName;
    private String phoneNumber;
}
