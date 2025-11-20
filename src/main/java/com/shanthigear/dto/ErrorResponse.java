package com.shanthigear.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Standard error response DTO for API error handling.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    /**
     * A machine-readable error code.
     */
    private String code;
    
    /**
     * A human-readable error message.
     */
    private String message;
    
    /**
     * Optional list of field-level errors.
     */
    private List<String> errors;
    
    /**
     * Creates a simple error response with just a message.
     */
    public static ErrorResponse of(String code, String message) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .build();
    }
    
    /**
     * Creates an error response with a message and error details.
     */
    public static ErrorResponse of(String code, String message, List<String> errors) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .errors(errors)
                .build();
    }
}
