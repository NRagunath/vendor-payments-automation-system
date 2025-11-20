package com.shanthigear.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * Utility class for security-related operations.
 * Provides methods for hashing, generating random strings, and accessing security context.
 */
@Slf4j
@UtilityClass
public class SecurityUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    /**
     * Gets the current authenticated username from the security context.
     *
     * @return An Optional containing the username if authenticated, empty otherwise
     */
    public static Optional<String> getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return Optional.of(((UserDetails) principal).getUsername());
        } else if (principal instanceof String) {
            return Optional.of((String) principal);
        }
        
        return Optional.empty();
    }

    /**
     * Checks if the current user has a specific role.
     *
     * @param role The role to check
     * @return true if the user has the role, false otherwise
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_" + role));
    }

    /**
     * Generates a cryptographically secure random string.
     *
     * @param length The length of the string to generate
     * @return A random string
     */
    public static String generateRandomString(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(SECURE_RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    /**
     * Generates a secure random password with the specified complexity.
     *
     * @param length The length of the password
     * @param includeSpecialChars Whether to include special characters
     * @return A secure random password
     */
    public static String generateSecurePassword(int length, boolean includeSpecialChars) {
        if (length < 8) {
            throw new IllegalArgumentException("Password length must be at least 8 characters");
        }

        String chars = includeSpecialChars ? ALPHANUMERIC + SPECIAL_CHARS : ALPHANUMERIC;
        StringBuilder password = new StringBuilder(length);
        
        // Ensure at least one of each required character type
        password.append(ALPHANUMERIC.charAt(10 + SECURE_RANDOM.nextInt(26))); // Uppercase
        password.append(ALPHANUMERIC.charAt(36 + SECURE_RANDOM.nextInt(26))); // Lowercase
        password.append(ALPHANUMERIC.charAt(SECURE_RANDOM.nextInt(10))); // Digit
        
        if (includeSpecialChars) {
            password.append(SPECIAL_CHARS.charAt(SECURE_RANDOM.nextInt(SPECIAL_CHARS.length())));
        }

        // Fill the rest of the password
        for (int i = password.length(); i < length; i++) {
            password.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }

        // Shuffle the characters
        char[] array = password.toString().toCharArray();
        for (int i = array.length - 1; i > 0; i--) {
            int index = SECURE_RANDOM.nextInt(i + 1);
            char temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }

        return new String(array);
    }

    /**
     * Hashes a string using SHA-256.
     *
     * @param input The string to hash
     * @return The hashed string in hexadecimal format
     */
    public static String hashSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            throw new RuntimeException("Hashing algorithm not available", e);
        }
    }

    /**
     * Creates a salted hash for password storage.
     *
     * @param password The password to hash
     * @param salt The salt to use
     * @return The hashed password with salt
     */
    public static String hashWithSalt(String password, String salt) {
        String saltedPassword = password + salt;
        return hashSha256(saltedPassword);
    }

    /**
     * Verifies a password against a hashed password with salt.
     *
     * @param password The password to verify
     * @param hashedPassword The hashed password to compare against
     * @param salt The salt used in the original hash
     * @return true if the password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String hashedPassword, String salt) {
        String hashedAttempt = hashWithSalt(password, salt);
        return MessageDigest.isEqual(hashedAttempt.getBytes(StandardCharsets.UTF_8), 
                                   hashedPassword.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a secure random salt.
     *
     * @param length The length of the salt in bytes
     * @return A base64-encoded salt string
     */
    public static String generateSalt(int length) {
        byte[] salt = new byte[length];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes The byte array to convert
     * @return A hexadecimal string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
