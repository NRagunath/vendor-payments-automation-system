package com.shanthigear.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Utility class for file operations.
 */
@Slf4j
public class FileUtils {

    private static final String[] ALLOWED_EXTENSIONS = {"xlsx", "xls"};
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * Validates the uploaded file.
     *
     * @param file The file to validate
     * @throws IllegalArgumentException if the file is invalid
     */
    public static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds the maximum allowed limit (10MB)");
        }

        // Check file extension
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (extension == null || !isAllowedExtension(extension.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type. Only Excel files (.xlsx, .xls) are allowed");
        }
    }

    /**
     * Saves the uploaded file to a temporary location.
     *
     * @param file The file to save
     * @return The path to the saved file
     * @throws IOException if an I/O error occurs
     */
    public static Path saveToTemp(MultipartFile file) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        String originalFilename = file.getOriginalFilename();
        String fileExtension = FilenameUtils.getExtension(originalFilename);
        String newFilename = String.format("vendor_import_%s.%s", UUID.randomUUID(), fileExtension);
        
        Path uploadPath = Paths.get(tempDir);
        Path filePath = uploadPath.resolve(newFilename);
        
        // Create directories if they don't exist
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Save the file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        log.debug("Saved uploaded file to: {}", filePath);
        return filePath;
    }

    /**
     * Deletes a file if it exists.
     *
     * @param filePath The path to the file to delete
     */
    public static void deleteFileIfExists(Path filePath) {
        if (filePath != null && Files.exists(filePath)) {
            try {
                Files.deleteIfExists(filePath);
                log.debug("Deleted temporary file: {}", filePath);
            } catch (IOException e) {
                log.warn("Failed to delete temporary file: {}", filePath, e);
            }
        }
    }

    /**
     * Checks if the file extension is allowed.
     */
    private static boolean isAllowedExtension(String extension) {
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
}
