package com.shanthigear.exception;

/**
 * Exception thrown when a payment request is invalid.
 */
public class InvalidPaymentException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new InvalidPaymentException with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the getMessage() method)
     */
    public InvalidPaymentException(String message) {
        super(message);
    }

    /**
     * Constructs a new InvalidPaymentException with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the getMessage() method)
     * @param cause the cause (which is saved for later retrieval by the getCause() method)
     */
    public InvalidPaymentException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new InvalidPaymentException with the specified cause and a detail message of
     * (cause==null ? null : cause.toString()) (which typically contains the class and detail message of cause).
     *
     * @param cause the cause (which is saved for later retrieval by the getCause() method)
     */
    public InvalidPaymentException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new InvalidPaymentException with the specified detail message,
     * cause, suppression enabled or disabled, and writable stack trace enabled or disabled.
     *
     * @param message the detail message
     * @param cause the cause
     * @param enableSuppression whether or not suppression is enabled or disabled
     * @param writableStackTrace whether or not the stack trace should be writable
     */
    protected InvalidPaymentException(String message, Throwable cause, 
                                    boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
