package com.shanthigear.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when there is an error during payment processing.
 */
public class PaymentProcessingException extends BaseException {
    private static final long serialVersionUID = 1L;
    
    public PaymentProcessingException(String message) {
        super(message);
    }
    
    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
    
    @Override
    public HttpStatus getStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR; // Default to 500 for payment processing errors
    }
}
