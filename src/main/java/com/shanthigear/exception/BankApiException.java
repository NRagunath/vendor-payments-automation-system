package com.shanthigear.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Exception thrown when there is an error communicating with the bank's API.
 */
public class BankApiException extends BaseException {
    private static final long serialVersionUID = 1L;
    private final HttpStatusCode statusCode;
    
    public BankApiException(String message) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    public BankApiException(String message, Throwable cause) {
        this(message, cause, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    public BankApiException(String message, HttpStatus status) {
        super(message);
        this.statusCode = status;
    }
    
    public BankApiException(String message, HttpStatusCode statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public BankApiException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.statusCode = status;
    }
    
    public BankApiException(String message, Throwable cause, HttpStatusCode statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }
    
    @Override
    public HttpStatusCode getStatus() {
        return statusCode != null ? statusCode : HttpStatus.INTERNAL_SERVER_ERROR;
    }
    
    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}
