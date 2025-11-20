package com.shanthigear.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

/**
 * Validator for the {@link ValidFile} annotation.
 * Validates file uploads based on size, extension, and content type constraints.
 */
@Slf4j
public class ValidFileValidator implements ConstraintValidator<ValidFile, MultipartFile> {

    private long maxSize;
    private String[] allowedExtensions;
    private String[] allowedContentTypes;

    @Override
    public void initialize(ValidFile constraint) {
        this.maxSize = constraint.maxSize();
        this.allowedExtensions = constraint.allowedExtensions();
        this.allowedContentTypes = constraint.allowedContentTypes();
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return true; // Use @NotNull or @NotEmpty for required files
        }

        // Check file size
        if (file.getSize() > maxSize) {
            String message = String.format("File size must be less than %d bytes", maxSize);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message)
                   .addConstraintViolation();
            return false;
        }

        // Check file extension
        if (allowedExtensions.length > 0) {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null) {
                String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1)
                        .toLowerCase();
                if (Arrays.stream(allowedExtensions)
                        .noneMatch(ext -> ext.equalsIgnoreCase(fileExtension))) {
                    String message = String.format("Invalid file type. Allowed types: %s", 
                            String.join(", ", allowedExtensions));
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(message)
                           .addConstraintViolation();
                    return false;
                }
            }
        }

        // Check content type
        if (allowedContentTypes.length > 0 && file.getContentType() != null) {
            String contentType = file.getContentType().toLowerCase();
            if (Arrays.stream(allowedContentTypes)
                    .noneMatch(allowed -> allowed.toLowerCase().equals(contentType))) {
                String message = String.format("Invalid content type. Allowed types: %s", 
                        String.join(", ", allowedContentTypes));
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(message)
                       .addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}
