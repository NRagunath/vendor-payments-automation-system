package com.shanthigear.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class VendorAlreadyExistsException extends RuntimeException {
    public VendorAlreadyExistsException(String message) {
        super(message);
    }

    public VendorAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
