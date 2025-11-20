package com.shanthigear.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when batch processing of payments fails.
 */
public class BatchProcessingException extends BaseException {
    private static final long serialVersionUID = 1L;
    private final HttpStatus status;
    
    public BatchProcessingException(String message) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    public BatchProcessingException(String message, Throwable cause) {
        this(message, cause, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    public BatchProcessingException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
    
    public BatchProcessingException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
    }
    
    @Override
    public HttpStatus getStatus() {
        return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
