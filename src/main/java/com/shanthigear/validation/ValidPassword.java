package com.shanthigear.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation for password strength.
 * Validates that a password meets the specified complexity requirements.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordConstraintValidator.class)
@Documented
public @interface ValidPassword {
    String message() default "Invalid Password";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * @return Minimum length of the password. Default is 8.
     */
    int minLength() default 8;
    
    /**
     * @return Whether the password must contain at least one digit. Default is true.
     */
    boolean requireDigit() default true;
    
    /**
     * @return Whether the password must contain at least one lowercase letter. Default is true.
     */
    boolean requireLowercase() default true;
    
    /**
     * @return Whether the password must contain at least one uppercase letter. Default is true.
     */
    boolean requireUppercase() default true;
    
    /**
     * @return Whether the password must contain at least one special character. Default is true.
     */
    boolean requireSpecialChar() default true;
    
    /**
     * @return Maximum allowed occurrences of the same character in a row. Default is 2.
     */
    int maxRepeatedChars() default 2;
}
