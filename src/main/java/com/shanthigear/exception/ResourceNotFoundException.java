package com.shanthigear.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends BaseException {
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new ResourceNotFoundException with a message indicating which resource was not found.
     *
     * @param resourceName the name of the resource that was not found (e.g., "Vendor", "Payment")
     * @param fieldName the name of the field that was used for the lookup
     * @param fieldValue the value of the field that was used for the lookup
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
