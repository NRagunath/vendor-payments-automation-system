package com.shanthigear.exception;

import java.util.List;

/**
 * Exception thrown when validation of an object fails.
 */
public class ValidationException extends RuntimeException {
    
    private final List<String> errors;
    
    public ValidationException(String message, List<String> errors) {
        super(message);
        this.errors = errors != null ? errors : List.of();
    }
    
    public ValidationException(String message, List<String> errors, Throwable cause) {
        super(message, cause);
        this.errors = errors != null ? errors : List.of();
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    @Override
    public String getMessage() {
        if (errors == null || errors.isEmpty()) {
            return super.getMessage();
        }
        return String.format("%s: %s", super.getMessage(), String.join(", ", errors));
    }
}
