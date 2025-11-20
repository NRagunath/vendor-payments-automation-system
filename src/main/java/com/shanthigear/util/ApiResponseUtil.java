package com.shanthigear.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for creating consistent API responses.
 * Provides methods for creating success and error responses with standardized formats.
 */
public class ApiResponseUtil {

    /**
     * Creates a successful response with data.
     *
     * @param data The data to include in the response
     * @param <T>  The type of the data
     * @return A ResponseEntity containing the success response
     */
    public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(
            ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build()
        );
    }

    /**
     * Creates a successful paginated response.
     *
     * @param page The Page object containing the paginated data
     * @param <T>  The type of the data
     * @return A ResponseEntity containing the paginated success response
     */
    public static <T> ResponseEntity<ApiResponse<PaginatedResponse<T>>> success(Page<T> page) {
        PaginatedResponse<T> paginatedResponse = PaginatedResponse.<T>builder()
            .content(page.getContent())
            .pageNumber(page.getNumber())
            .pageSize(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .last(page.isLast())
            .first(page.isFirst())
            .empty(page.isEmpty())
            .build();

        return success(paginatedResponse);
    }

    /**
     * Creates a success response with a message.
     *
     * @param message The success message
     * @return A ResponseEntity containing the success message
     */
    public static ResponseEntity<ApiResponse<Void>> success(String message) {
        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build()
        );
    }

    /**
     * Creates an error response with a status code and message.
     *
     * @param status  The HTTP status code
     * @param message The error message
     * @return A ResponseEntity containing the error response
     */
    public static ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(
            ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build()
        );
    }

    /**
     * Creates an error response with validation errors.
     *
     * @param status       The HTTP status code
     * @param message      The error message
     * @param fieldErrors  The list of field errors
     * @return A ResponseEntity containing the validation error response
     */
    public static ResponseEntity<ApiResponse<Void>> validationError(
            HttpStatus status, String message, List<FieldError> fieldErrors) {
        
        return ResponseEntity.status(status).body(
            ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .errors(fieldErrors)
                .timestamp(LocalDateTime.now())
                .build()
        );
    }

    /**
     * Creates a not found response.
     *
     * @param message The error message
     * @return A 404 Not Found response
     */
    public static ResponseEntity<ApiResponse<Void>> notFound(String message) {
        return error(HttpStatus.NOT_FOUND, message);
    }

    /**
     * Creates a bad request response.
     *
     * @param message The error message
     * @return A 400 Bad Request response
     */
    public static ResponseEntity<ApiResponse<Void>> badRequest(String message) {
        return error(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Creates an unauthorized response.
     *
     * @param message The error message
     * @return A 401 Unauthorized response
     */
    public static ResponseEntity<ApiResponse<Void>> unauthorized(String message) {
        return error(HttpStatus.UNAUTHORIZED, message);
    }

    /**
     * Creates a forbidden response.
     *
     * @param message The error message
     * @return A 403 Forbidden response
     */
    public static ResponseEntity<ApiResponse<Void>> forbidden(String message) {
        return error(HttpStatus.FORBIDDEN, message);
    }

    /**
     * Creates an internal server error response.
     *
     * @param message The error message
     * @return A 500 Internal Server Error response
     */
    public static ResponseEntity<ApiResponse<Void>> internalServerError(String message) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    /**
     * Generic API response wrapper.
     *
     * @param <T> The type of the data
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonPropertyOrder({"success", "message", "data", "errors", "timestamp"})
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<FieldError> errors;
        
        private LocalDateTime timestamp;
        
        public static <T> ApiResponseBuilder<T> builder() {
            return new ApiResponseBuilder<>();
        }
    }

    /**
     * Field error details for validation errors.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String code;
        private String message;
        private Object rejectedValue;
    }

    /**
     * Paginated response wrapper.
     *
     * @param <T> The type of the content
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginatedResponse<T> {
        private List<T> content;
        private int pageNumber;
        private int pageSize;
        private long totalElements;
        private int totalPages;
        private boolean last;
        private boolean first;
        private boolean empty;
        
        public static <T> PaginatedResponse<T> empty() {
            return PaginatedResponse.<T>builder()
                .content(Collections.emptyList())
                .pageNumber(0)
                .pageSize(0)
                .totalElements(0)
                .totalPages(0)
                .last(true)
                .first(true)
                .empty(true)
                .build();
        }
    }
}
