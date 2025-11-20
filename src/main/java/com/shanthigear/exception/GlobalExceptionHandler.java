package com.shanthigear.exception;

import com.shanthigear.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Global exception handler for the application.
 * Handles various types of exceptions and returns appropriate error responses.
 */
/**
 * Global exception handler that provides centralized exception handling across all @RequestMapping methods.
 * Handles validation, data integrity, and custom business exceptions.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Handles ImportException and returns a 400 Bad Request response.
     */
    /**
     * Handles SSL handshake exceptions that occur during bank API communication.
     */
    @ExceptionHandler(BankApiException.class)
    public ResponseEntity<ErrorResponseDTO> handleBankApiException(
            BankApiException ex, HttpServletRequest request) {
        log.error("Bank API error: {}", ex.getMessage(), ex);
        
        HttpStatus status = ex.getStatus() != null ? 
            (ex.getStatus() instanceof HttpStatus ? 
                (HttpStatus) ex.getStatus() : 
                HttpStatus.valueOf(ex.getStatus().value())) : 
            HttpStatus.INTERNAL_SERVER_ERROR;
        
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
                
        return new ResponseEntity<>(errorResponse, status);
    }
    
    @ExceptionHandler(javax.net.ssl.SSLHandshakeException.class)
    public ResponseEntity<ErrorResponseDTO> handleSslHandshakeException(
            javax.net.ssl.SSLHandshakeException ex, HttpServletRequest request) {
        
        log.error("SSL Handshake failed: {}", ex.getMessage(), ex);
        
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_GATEWAY.value())
                .error("SSL Handshake Failed")
                .message("Failed to establish a secure connection to the bank's server. Please check SSL configuration.")
                .path(request.getRequestURI())
                .build();
                
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_GATEWAY);
    }
    
    /**
     * Handles connection timeouts when communicating with the bank API.
     */
    @ExceptionHandler(org.springframework.web.client.ResourceAccessException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceAccessException(
            org.springframework.web.client.ResourceAccessException ex, HttpServletRequest request) {
        
        log.error("Connection to bank API failed: {}", ex.getMessage(), ex);
        
        String message = "Unable to connect to the bank's server. ";
        if (ex.getCause() instanceof java.net.ConnectException) {
            message += "The server may be down or unreachable.";
        } else if (ex.getCause() instanceof java.net.SocketTimeoutException) {
            message += "The connection timed out. Please try again later.";
        } else {
            message += ex.getMessage();
        }
        
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.GATEWAY_TIMEOUT.value())
                .error("Connection Error")
                .message(message)
                .path(request.getRequestURI())
                .build();
                
        return new ResponseEntity<>(errorResponse, HttpStatus.GATEWAY_TIMEOUT);
    }
    
    /**
     * Handles HTTP client errors (4xx) from the bank API.
     */
    @ExceptionHandler(org.springframework.web.client.HttpClientErrorException.class)
    public ResponseEntity<ErrorResponseDTO> handleHttpClientErrorException(
            org.springframework.web.client.HttpClientErrorException ex, HttpServletRequest request) {
        
        log.error("Bank API client error ({}): {}", ex.getStatusCode(), ex.getMessage());
        
        String message = "Error from bank API: " + ex.getStatusText();
        if (ex.getResponseBodyAsString() != null && !ex.getResponseBodyAsString().isEmpty()) {
            message += " - " + ex.getResponseBodyAsString();
        }
        
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatusCode().value())
                .error("Bank API Error")
                .message(message)
                .path(request.getRequestURI())
                .build();
                
        return new ResponseEntity<>(errorResponse, ex.getStatusCode());
    }
    
    /**
     * Handles HTTP server errors (5xx) from the bank API.
     */
    @ExceptionHandler(org.springframework.web.client.HttpServerErrorException.class)
    public ResponseEntity<ErrorResponseDTO> handleHttpServerErrorException(
            org.springframework.web.client.HttpServerErrorException ex, HttpServletRequest request) {
        
        log.error("Bank API server error ({}): {}", ex.getStatusCode(), ex.getMessage(), ex);
        
        String message = "Bank service is currently unavailable. Please try again later.";
        if (ex.getResponseBodyAsString() != null && !ex.getResponseBodyAsString().isEmpty()) {
            // Be careful not to expose internal error details
            log.debug("Bank API error details: {}", ex.getResponseBodyAsString());
        }
        
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message(message)
                .path(request.getRequestURI())
                .build();
                
        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }


    @ExceptionHandler(VendorAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDTO> handleVendorAlreadyExistsException(
            VendorAlreadyExistsException ex, WebRequest request) {
        log.warn("Vendor already exists: {}", ex.getMessage());
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.CONFLICT.value())
            .error(HttpStatus.CONFLICT.getReasonPhrase())
            .message(ex.getMessage())
            .path(((ServletWebRequest)request).getRequest().getRequestURI())
            .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidVendorDataException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidVendorDataException(
            InvalidVendorDataException ex, WebRequest request) {
        log.warn("Invalid vendor data: {}", ex.getMessage());
        
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message(ex.getMessage())
            .path(((ServletWebRequest)request).getRequest().getRequestURI())
            .build();
            
        if (ex.getValidationErrors() != null && !ex.getValidationErrors().isEmpty()) {
            // Add all validation errors to the errors map
            for (int i = 0; i < ex.getValidationErrors().size(); i++) {
                errorResponse.addError("error" + (i + 1), ex.getValidationErrors().get(i));
            }
        }
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ImportException.class)
    public ResponseEntity<ErrorResponseDTO> handleImportException(ImportException ex, WebRequest request) {
        log.warn("Import error: {}", ex.getMessage());
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message(ex.getMessage())
            .path(((ServletWebRequest)request).getRequest().getRequestURI())
            .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles MaxUploadSizeExceededException and returns a 400 Bad Request response.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDTO> handleMaxSizeException(MaxUploadSizeExceededException ex, WebRequest request) {
        log.warn("File size exceeds limit: {}", ex.getMessage());
        
        String path = "";
        if (request instanceof ServletWebRequest) {
            path = ((ServletWebRequest)request).getRequest().getRequestURI();
        }
        
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message("File size exceeds the maximum allowed limit")
            .path(path)
            .build();
            
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles ConstraintViolationException and returns a 400 Bad Request response.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());
        
        // Create a new LinkedHashMap to collect the errors
        Map<String, Object> errorMap = new LinkedHashMap<>();
        
        // Process constraint violations
        ex.getConstraintViolations().forEach(violation -> 
            errorMap.put(
                violation.getPropertyPath().toString(),
                Objects.requireNonNullElse(violation.getMessage(), "")
            )
        );

        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Constraint Violation")
            .message("Validation error. Check 'errors' field for details.")
            .path(getRequestPath(request))
            .errors(errorMap)
            .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles IllegalStateException and returns a 400 Bad Request response.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {
        log.warn("Illegal state: {}", ex.getMessage());
        
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .message(ex.getMessage())
            .path(getRequestPath(request))
            .build();
            
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles IllegalArgumentException and returns a 400 Bad Request response.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Invalid Argument")
            .message(ex.getMessage())
            .path(getRequestPath(request))
            .build();
            
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles IllegalArgumentException and returns a 400 Bad Request response.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        log.error("Method argument type mismatch: {}", ex.getMessage(), ex);
        
        // Safely get the required type name
        String requiredType = "unknown";
        Class<?> requiredTypeClass = ex.getRequiredType();
        if (requiredTypeClass != null) {
            requiredType = requiredTypeClass.getSimpleName();
        }
        
        String error = String.format("Parameter '%s' requires a value of type: %s", 
            ex.getName(), requiredType);
        
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Type Mismatch")
            .message(error)
            .path(getRequestPath(request))
            .build();
            
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, WebRequest request) {
        log.error("Data integrity violation: {}", ex.getMessage(), ex);
        String message = "Data integrity violation";
        
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            message = ex.getCause().getMessage();
            // Extract specific error message for common constraint violations
            if (message.contains("duplicate key")) {
                message = "Duplicate entry. This record already exists.";
            } else if (message.contains("foreign key constraint")) {
                message = "Cannot delete or update a parent row: a foreign key constraint fails";
            }
        }
        
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.CONFLICT.value())
            .error("Data Integrity Violation")
            .message(message)
            .path(getRequestPath(request))
            .build();
            
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Access Denied")
            .message("You don't have permission to access this resource")
            .path(getRequestPath(request))
            .build();
            
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            @NonNull HttpMessageNotReadableException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        
        String errorMessage = "Request body is not valid JSON";
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            errorMessage = ex.getCause().getMessage();
            // Clean up the message if it contains class information
            if (errorMessage.contains("java.") || errorMessage.contains("@")) {
                errorMessage = "Invalid request format";
            }
        }
        
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error("Malformed JSON request")
            .message(errorMessage)
            .path(getRequestPath(request))
            .build();
            
        return handleExceptionInternal(ex, errorResponse, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            @NonNull MissingServletRequestParameterException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        log.warn("Missing request parameter: {}", ex.getMessage());
        
        String parameterType = ex.getParameterType();
        String message = String.format(
            "Required %s parameter '%s' is not present", 
            parameterType != null ? parameterType : "", 
            ex.getParameterName()
        );
        
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error("Missing Request Parameter")
            .message(message)
            .path(getRequestPath(request))
            .build();
            
        return handleExceptionInternal(ex, errorResponse, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            @NonNull NoHandlerFoundException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        log.warn("No handler found for {} {}", ex.getHttpMethod(), ex.getRequestURL());
        
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error("Not Found")
            .message(ex.getMessage() != null ? ex.getMessage() : "The requested resource was not found")
            .path(getRequestPath(request))
            .build();
            
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Extracts the request path from WebRequest
     */
    private String getRequestPath(WebRequest request) {
        if (request == null) {
            return "";
        }
        if (request instanceof ServletWebRequest) {
            HttpServletRequest httpRequest = ((ServletWebRequest) request).getRequest();
            return httpRequest != null ? httpRequest.getRequestURI() : "";
        }
        String contextPath = request.getContextPath();
        return contextPath != null ? contextPath : "";
    }
}
