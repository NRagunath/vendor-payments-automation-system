package com.shanthigear.exception;

/**
 * Exception thrown when there is an error processing an invoice.
 */
public class InvoiceProcessingException extends Exception {
    
    public InvoiceProcessingException(String message) {
        super(message);
    }
    
    public InvoiceProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
