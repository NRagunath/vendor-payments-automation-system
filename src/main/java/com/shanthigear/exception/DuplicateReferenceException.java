package com.shanthigear.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a duplicate reference is detected.
 */
public class DuplicateReferenceException extends BaseException {
    private static final long serialVersionUID = 1L;
    
    public DuplicateReferenceException(String message) {
        super(message);
    }
    
    public DuplicateReferenceException(String message, Throwable cause) {
        super(message, cause);
    }
    
    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
