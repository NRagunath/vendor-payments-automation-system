package com.shanthigear.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = VendorNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidVendorNumber {
    String message() default "Vendor number must be between 10005 and 144617";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
