package com.shanthigear.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import java.util.regex.Pattern;

/**
 * Utility class for secure logging of sensitive information.
 * Prevents accidental logging of sensitive data like PAN, CVV, passwords, etc.
 */
public class SecureLoggingUtils {

    // Patterns for sensitive data
    private static final Pattern PAN_PATTERN = Pattern.compile("\\b(?:4[0-9]{12}(?:[0-9]{3})?|(?:5[1-5][0-9]{2}|222[1-9]|22[3-9][0-9]|2[3-6][0-9]{2}|27[01][0-9]|2720)[0-9]{12}|3[47][0-9]{13}|3(?:0[0-5]|[68][0-9])[0-9]{11}|6(?:011|5[0-9]{2})[0-9]{12}|(?:2131|1800|35\\d{3})\\d{11})\\b");
    private static final Pattern CVV_PATTERN = Pattern.compile("\\b[0-9]{3,4}\\b");
    private static final Pattern API_KEY_PATTERN = Pattern.compile("(?i)(?:api[_-]?key|auth[_-]?token|bearer)\\s*[:=]\\s*([a-z0-9_\\-.]{20,100})", Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(?i)(?:password|pwd|pass|secret)\\s*[:=]\\s*([^\\s&\"']+)", Pattern.CASE_INSENSITIVE);
    
    // Masking characters
    private static final String MASKED_VALUE = "[MASKED]";
    private static final int VISIBLE_CHARS = 4;
    
    private SecureLoggingUtils() {
        // Utility class, prevent instantiation
    }
    
    /**
     * Masks sensitive information in the input string.
     * @param input The string that might contain sensitive information
     * @return A string with sensitive information masked
     */
    public static String maskSensitiveData(String input) {
        if (StringUtils.isBlank(input)) {
            return input;
        }
        
        // Mask PAN numbers
        input = PAN_PATTERN.matcher(input).replaceAll(match -> maskString(match.group()));
        
        // Mask CVV numbers
        input = CVV_PATTERN.matcher(input).replaceAll(MASKED_VALUE);
        
        // Mask API keys and tokens
        input = API_KEY_PATTERN.matcher(input).replaceAll("$1: " + MASKED_VALUE);
        
        // Mask passwords
        input = PASSWORD_PATTERN.matcher(input).replaceAll("$1: " + MASKED_VALUE);
        
        return input;
    }
    
    /**
     * Masks a string, showing only the first few and last few characters.
     * @param value The string to mask
     * @return The masked string
     */
    public static String maskString(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        
        int length = value.length();
        if (length <= VISIBLE_CHARS * 2) {
            return StringUtils.repeat('*', length);
        }
        
        String start = value.substring(0, VISIBLE_CHARS);
        String end = value.substring(length - VISIBLE_CHARS);
        return start + StringUtils.repeat('*', length - VISIBLE_CHARS * 2) + end;
    }
    
    /**
     * Safely logs a message with sensitive information masked.
     * @param logger The logger to use
     * @param format The message format
     * @param args The message arguments
     */
    public static void info(Logger logger, String format, Object... args) {
        if (logger.isInfoEnabled()) {
            logger.info(maskSensitiveData(String.format(format, args)));
        }
    }
    
    /**
     * Safely logs a debug message with sensitive information masked.
     * @param logger The logger to use
     * @param format The message format
     * @param args The message arguments
     */
    public static void debug(Logger logger, String format, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(maskSensitiveData(String.format(format, args)));
        }
    }
    
    /**
     * Safely logs an error message with sensitive information masked.
     * @param logger The logger to use
     * @param message The error message
     * @param t The throwable
     */
    public static void error(Logger logger, String message, Throwable t) {
        if (logger.isErrorEnabled()) {
            logger.error(maskSensitiveData(message), t);
        }
    }
    
    /**
     * Safely logs an error message with sensitive information masked.
     * @param logger The logger to use
     * @param format The message format
     * @param args The message arguments
     */
    public static void error(Logger logger, String format, Object... args) {
        if (logger.isErrorEnabled()) {
            logger.error(maskSensitiveData(String.format(format, args)));
        }
    }
}
