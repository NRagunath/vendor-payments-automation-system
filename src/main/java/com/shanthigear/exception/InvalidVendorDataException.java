package com.shanthigear.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidVendorDataException extends RuntimeException {
    private final List<String> validationErrors;

    public InvalidVendorDataException(String message) {
        super(message);
        this.validationErrors = null;
    }

    public InvalidVendorDataException(String message, List<String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
