package com.shanthigear.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when there is an error sending notifications.
 */
public class NotificationException extends BaseException {
    private static final long serialVersionUID = 1L;
    
    public NotificationException(String message) {
        super(message);
    }
    
    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    @Override
    public HttpStatus getStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
