package com.shanthigear.exception;

/**
 * Exception thrown when there is an error communicating with the bank's API.
 */
public class BankIntegrationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BankIntegrationException(String message) {
        super(message);
    }

    public BankIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BankIntegrationException(Throwable cause) {
        super(cause);
    }
}
