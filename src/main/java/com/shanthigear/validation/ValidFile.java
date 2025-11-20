package com.shanthigear.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation for file uploads.
 * Validates that the uploaded file meets the specified constraints.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidFileValidator.class)
@Documented
public @interface ValidFile {
    String message() default "Invalid file";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * @return The maximum file size in bytes. Defaults to 5MB.
     */
    long maxSize() default 5 * 1024 * 1024; // 5MB default
    
    /**
     * @return The allowed file extensions (e.g., {"pdf", "jpg", "png"}).
     *         If empty, all file types are allowed.
     */
    String[] allowedExtensions() default {};
    
    /**
     * @return The allowed content types (e.g., {"application/pdf", "image/jpeg"}).
     *         If empty, all content types are allowed.
     */
    String[] allowedContentTypes() default {};
}
