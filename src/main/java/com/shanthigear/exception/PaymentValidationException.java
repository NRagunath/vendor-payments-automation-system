package com.shanthigear.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when payment validation fails.
 */
public class PaymentValidationException extends BaseException {
    private static final long serialVersionUID = 1L;
    private final HttpStatus status;
    
    public PaymentValidationException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }
    
    public PaymentValidationException(String message, Throwable cause) {
        this(message, cause, HttpStatus.BAD_REQUEST);
    }
    
    public PaymentValidationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
    
    public PaymentValidationException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
    }
    
    @Override
    public HttpStatus getStatus() {
        return status != null ? status : HttpStatus.BAD_REQUEST;
    }
}
