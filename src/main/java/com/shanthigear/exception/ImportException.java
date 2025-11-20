package com.shanthigear.exception;

/**
 * Exception thrown when there is an error during vendor import.
 */
public class ImportException extends RuntimeException {
    
    public ImportException(String message) {
        super(message);
    }
    
    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
