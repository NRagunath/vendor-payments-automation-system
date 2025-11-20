package com.shanthigear.model;

/**
 * Represents the result of an email sending operation.
 */
public class EmailSendingResult {
    private final boolean success;
    private final String message;
    private final String errorDetails;
    private final String email;
    private final long timestamp;

    public EmailSendingResult(boolean success, String message) {
        this(success, message, null, null);
    }

    public EmailSendingResult(boolean success, String message, String errorDetails) {
        this(success, message, errorDetails, null);
    }

    public EmailSendingResult(boolean success, String message, String errorDetails, String email) {
        this.success = success;
        this.message = message != null ? message : "";
        this.errorDetails = errorDetails != null ? errorDetails : "";
        this.email = email;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public String getEmail() {
        return email;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "EmailSendingResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", errorDetails='" + errorDetails + '\'' +
                ", email='" + email + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
