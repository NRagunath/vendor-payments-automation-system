package com.shanthigear.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class VendorNumberValidator implements ConstraintValidator<ValidVendorNumber, String> {
    private static final int MIN_VENDOR_NUMBER = 10005;
    private static final int MAX_VENDOR_NUMBER = 144617;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        
        try {
            int vendorNumber = Integer.parseInt(value);
            return vendorNumber >= MIN_VENDOR_NUMBER && vendorNumber <= MAX_VENDOR_NUMBER;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
