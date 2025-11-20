package com.shanthigear.controller;

import com.shanthigear.dto.UserRegistrationDto;
import com.shanthigear.service.UserService;
import com.shanthigear.util.ApiResponseUtil;
import com.shanthigear.util.DateTimeUtil;
import com.shanthigear.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * REST controller for managing users.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for user management")
@Validated
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<?> registerUser(@Validated @RequestBody UserRegistrationDto registrationDto) {
        // The @ValidPassword annotation will automatically validate the password
        // before this method is called
        
        // Generate a random salt and hash the password
        String salt = SecurityUtil.generateRandomString(16);
        // Hash the password (result not used in this example)
        SecurityUtil.hashWithSalt(registrationDto.getPassword(), salt);
        
        // In a real app, save the user with hashed password and salt
        // userService.registerUser(registrationDto, hashedPassword, salt);
        
        // Return success response with created user data
        return ApiResponseUtil.success(
            Map.of(
                "message", "User registered successfully",
                "timestamp", DateTimeUtil.nowUtc(),
                "username", registrationDto.getUsername(),
                "email", registrationDto.getEmail()
            )
        );
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<?> getUserProfile() {
        // Get current username from security context
        Optional<String> usernameOpt = SecurityUtil.getCurrentUsername();
        
        if (usernameOpt.isEmpty()) {
            return ApiResponseUtil.unauthorized("User not authenticated");
        }
        
        String username = usernameOpt.get();
        // In a real app, fetch user from database
        // User user = userService.findByUsername(username);
        return ApiResponseUtil.success(
            Map.of(
                "username", username,
                "email", username + "@example.com", // Replace with actual user data
                "roles", "ROLE_USER", // Replace with actual roles
                "lastLogin", DateTimeUtil.nowUtc().minusHours(2), // Example
                "subscriptionDetails", userService.getUserSubscriptionDetails(username)
            )
        );
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin dashboard", description = "Accessible only by admins")
    public ResponseEntity<?> adminDashboard() {
        return ApiResponseUtil.success(
            Map.of(
                "message", "Welcome to Admin Dashboard",
                "serverTime", DateTimeUtil.nowUtc(),
                "activeSessions", 42, // Example data
                "systemHealth", "OK",
                "features", Map.of(
                    "userManagement", true,
                    "analytics", true,
                    "reports", true
                )
            )
        );
    }

    @GetMapping("/password/strength")
    @Operation(summary = "Check password strength")
    public ResponseEntity<?> checkPasswordStrength(
            @RequestParam String password,
            @RequestParam(required = false, defaultValue = "true") boolean requireSpecial) {
        
        boolean isStrong = password.length() >= 8 &&
                          password.matches(".*[A-Z].*") && // has uppercase
                          password.matches(".*[a-z].*") && // has lowercase
                          password.matches(".*\\d.*") &&   // has digit
                          (!requireSpecial || password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\\\"\\\\|,.<>/?].*")); // has special char
        
        return ApiResponseUtil.success(
            Map.of(
                "strength", isStrong ? "strong" : "weak",
                "length", password.length(),
                "hasUppercase", password.matches(".*[A-Z].*"),
                "hasLowercase", password.matches(".*[a-z].*"),
                "hasDigit", password.matches(".*\\d.*"),
                "hasSpecial", password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\\\"\\\\|,.<>/?].*")
            )
        );
    }
    
    @GetMapping("/{username}/subscription")
    @Operation(summary = "Get user subscription details")
    public ResponseEntity<?> getUserSubscription(@PathVariable String username) {
        // In a real app, add authorization to check if the current user can access this data
        return ApiResponseUtil.success(userService.getUserSubscriptionDetails(username));
    }
}
