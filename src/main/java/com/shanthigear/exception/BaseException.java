package com.shanthigear.exception;

import org.springframework.http.HttpStatusCode;

/**
 * Base exception class for the application.
 */
public abstract class BaseException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    public BaseException(String message) {
        super(message);
    }
    
    public BaseException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Returns the HTTP status code associated with this exception.
     *
     * @return the HTTP status code
     */
    public abstract HttpStatusCode getStatus();
}
