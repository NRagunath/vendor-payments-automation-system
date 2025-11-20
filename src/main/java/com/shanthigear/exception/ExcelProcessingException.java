package com.shanthigear.exception;

/**
 * Exception thrown when there is an error processing Excel files.
 */
public class ExcelProcessingException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    public ExcelProcessingException(String message) {
        super(message);
    }

    public ExcelProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
