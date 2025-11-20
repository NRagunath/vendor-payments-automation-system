package com.shanthigear.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a payment is not found.
 */
public class PaymentNotFoundException extends BaseException {
    private static final long serialVersionUID = 1L;
    private final HttpStatus status;
    
    public PaymentNotFoundException(String message) {
        this(message, HttpStatus.NOT_FOUND);
    }
    
    public PaymentNotFoundException(String message, Throwable cause) {
        this(message, cause, HttpStatus.NOT_FOUND);
    }
    
    public PaymentNotFoundException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
    
    public PaymentNotFoundException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
    }
    
    @Override
    public HttpStatus getStatus() {
        return status != null ? status : HttpStatus.NOT_FOUND;
    }
}
