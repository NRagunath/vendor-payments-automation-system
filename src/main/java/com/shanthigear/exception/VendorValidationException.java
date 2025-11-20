package com.shanthigear.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when vendor validation fails.
 */
public class VendorValidationException extends BaseException {
    private static final long serialVersionUID = 1L;
    private final HttpStatus status;
    
    public VendorValidationException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }
    
    public VendorValidationException(String message, Throwable cause) {
        this(message, cause, HttpStatus.BAD_REQUEST);
    }
    
    public VendorValidationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
    
    public VendorValidationException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
    }
    
    @Override
    public HttpStatus getStatus() {
        return status != null ? status : HttpStatus.BAD_REQUEST;
    }
}
