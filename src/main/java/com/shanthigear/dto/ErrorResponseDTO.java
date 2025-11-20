package com.shanthigear.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data Transfer Object for error responses.
 * Standardizes the format of error responses across the application.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class ErrorResponseDTO {
    
    /**
     * The timestamp when the error occurred.
     */
    private LocalDateTime timestamp;
    
    /**
     * The HTTP status code.
     */
    private int status;
    
    /**
     * The HTTP status reason phrase.
     */
    private String error;
    
    /**
     * A brief message describing the error.
     */
    private String message;
    
    /**
     * The path where the error occurred.
     */
    private String path;
    
    /**
     * Optional field for additional error details or validation errors.
     */
    @Builder.Default
    private Map<String, Object> errors = new LinkedHashMap<>();
    
    /**
     * Constructor without details.
     */
    public ErrorResponseDTO(
            LocalDateTime timestamp, 
            int status, 
            String error, 
            String message, 
            String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }
    
    /**
     * Creates an error response for validation failures.
     */
    public static ErrorResponseDTO validationError(String message, Map<String, String> fieldErrors) {
        return ErrorResponseDTO.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Error")
            .message(message)
            .errors(Collections.unmodifiableMap(fieldErrors))
            .build();
    }
    
    /**
     * Creates an error response for resource not found scenarios.
     */
    public static ErrorResponseDTO notFound(String resourceName, Object identifier) {
        return ErrorResponseDTO.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .message(String.format("%s with id %s not found", resourceName, identifier))
            .build();
    }
    
    /**
     * Creates an error response for unauthorized access.
     */
    public static ErrorResponseDTO unauthorized(String message) {
        return ErrorResponseDTO.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("Unauthorized")
            .message(message)
            .build();
    }
    
    /**
     * Creates an error response for internal server errors.
     */
    public static ErrorResponseDTO internalServerError(String message) {
        return ErrorResponseDTO.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message(message)
            .build();
    }
    
    /**
     * Adds an error to the errors map.
     * @param key The error key (e.g., field name)
     * @param value The error message
     * @return This instance for method chaining
     */
    public ErrorResponseDTO addError(String key, Object value) {
        if (this.errors == null) {
            this.errors = new LinkedHashMap<>();
        }
        this.errors.put(key, value);
        return this;
    }
    
    /**
     * Sets the path where the error occurred.
     * @param path The request path
     * @return This instance for method chaining
     */
    public ErrorResponseDTO withPath(String path) {
        this.path = path;
        return this;
    }
    
    @Override
    public String toString() {
        return "ErrorResponseDTO{" +
                "timestamp=" + timestamp +
                ", status=" + status +
                ", error='" + error + '\'' +
                ", message='" + message + '\'' +
                (path != null ? ", path='" + path + '\'' : "") +
                (!errors.isEmpty() ? ", errors=" + errors : "") +
                '}';
    }
}
