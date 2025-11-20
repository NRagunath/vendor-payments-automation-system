package com.shanthigear.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * Validator for the {@link ValidPassword} annotation.
 * Enforces password complexity rules including length, character types, and common patterns.
 */
@Slf4j
public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    private ValidPassword constraint;
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+\\-={}[]|:;\"'<>,.?/`~";

    @Override
    public void initialize(ValidPassword constraint) {
        this.constraint = constraint;
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }

        // Check minimum length
        if (password.length() < constraint.minLength()) {
            addValidationError(context, "Password must be at least " + constraint.minLength() + " characters long");
            return false;
        }

        // Check for at least one digit
        if (constraint.requireDigit() && !password.matches(".*\\d.*")) {
            addValidationError(context, "Password must contain at least one digit");
            return false;
        }

        // Check for at least one lowercase letter
        if (constraint.requireLowercase() && !password.matches(".*[a-z].*")) {
            addValidationError(context, "Password must contain at least one lowercase letter");
            return false;
        }

        // Check for at least one uppercase letter
        if (constraint.requireUppercase() && !password.matches(".*[A-Z].*")) {
            addValidationError(context, "Password must contain at least one uppercase letter");
            return false;
        }

        // Check for at least one special character
        if (constraint.requireSpecialChar() && !containsSpecialChar(password)) {
            addValidationError(context, "Password must contain at least one special character: " + SPECIAL_CHARS);
            return false;
        }

        // Check for repeated characters
        if (hasRepeatedChars(password, constraint.maxRepeatedChars())) {
            addValidationError(context, "Password contains too many repeated characters (max " + constraint.maxRepeatedChars() + " allowed)");
            return false;
        }

        // Check for common patterns (e.g., sequential numbers, repeated patterns)
        if (isCommonPattern(password)) {
            addValidationError(context, "Password is too common or follows a simple pattern");
            return false;
        }

        return true;
    }

    private boolean containsSpecialChar(String password) {
        return password.chars().anyMatch(ch -> SPECIAL_CHARS.indexOf(ch) >= 0);
    }

    private boolean hasRepeatedChars(String password, int maxRepeated) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        char prevChar = password.charAt(0);
        int count = 1;

        for (int i = 1; i < password.length(); i++) {
            char currentChar = password.charAt(i);
            if (currentChar == prevChar) {
                count++;
                if (count > maxRepeated) {
                    return true;
                }
            } else {
                count = 1;
                prevChar = currentChar;
            }
        }
        return false;
    }

    private boolean isCommonPattern(String password) {
        // Check for sequential numbers (123456, 987654, etc.)
        if (password.matches("\\d+")) {
            boolean sequential = true;
            boolean reverseSequential = true;
            
            for (int i = 1; i < password.length(); i++) {
                int current = Character.getNumericValue(password.charAt(i));
                int previous = Character.getNumericValue(password.charAt(i - 1));
                
                if (current != previous + 1) {
                    sequential = false;
                }
                if (current != previous - 1) {
                    reverseSequential = false;
                }
                
                if (!sequential && !reverseSequential) {
                    break;
                }
            }
            
            if (sequential || reverseSequential) {
                return true;
            }
        }
        
        // Check for repeated patterns (e.g., abcabc, 123123)
        for (int i = 1; i <= password.length() / 2; i++) {
            String pattern = password.substring(0, i);
            if (password.matches("(" + Pattern.quote(pattern) + "){2,}")) {
                return true;
            }
        }
        
        return false;
    }

    private void addValidationError(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }
}
