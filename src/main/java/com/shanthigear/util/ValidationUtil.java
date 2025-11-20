package com.shanthigear.util;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for performing validation on objects.
 * Provides methods to validate objects and return detailed error messages.
 */
@Slf4j
@UtilityClass
public class ValidationUtil {

    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();

    /**
     * Validates an object and returns a list of error messages if validation fails.
     * @param object The object to validate
     * @return A list of error messages, or an empty list if validation passes
     */
    public static <T> Set<String> validate(T object) {
        if (object == null) {
            return Set.of("Object to validate cannot be null");
        }
        
        return validator.validate(object).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    /**
     * Validates an object and throws a ValidationException if validation fails.
     * @param object The object to validate
     * @throws jakarta.validation.ValidationException if validation fails
     */
    public static <T> void validateAndThrow(T object) {
        if (object == null) {
            throw new jakarta.validation.ValidationException("Object to validate cannot be null");
        }
        
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(v -> String.format("%s: %s", v.getPropertyPath(), v.getMessage()))
                    .collect(Collectors.joining(", "));
            throw new jakarta.validation.ValidationException(errorMessage);
        }
    }

    /**
     * Validates that a string is not blank.
     * @param value The string to validate
     * @param fieldName The name of the field for error messages
     * @throws IllegalArgumentException if the string is blank
     */
    public static void validateNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
    }

    /**
     * Validates that an object is not null.
     * @param object The object to validate
     * @param fieldName The name of the field for error messages
     * @throws IllegalArgumentException if the object is null
     */
    public static void validateNotNull(Object object, String fieldName) {
        if (object == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
    }

    /**
     * Validates that a number is positive.
     * @param number The number to validate
     * @param fieldName The name of the field for error messages
     * @throws IllegalArgumentException if the number is not positive
     */
    public static void validatePositive(Number number, String fieldName) {
        if (number == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        if (number.doubleValue() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    /**
     * Validates that a string matches a regular expression pattern.
     * @param value The string to validate
     * @param pattern The regular expression pattern
     * @param fieldName The name of the field for error messages
     * @param errorMessage The error message to use if validation fails
     * @throws IllegalArgumentException if the string does not match the pattern
     */
    public static void validatePattern(String value, String pattern, String fieldName, String errorMessage) {
        if (value != null && !value.matches(pattern)) {
            throw new IllegalArgumentException(fieldName + " " + errorMessage);
        }
    }
}
