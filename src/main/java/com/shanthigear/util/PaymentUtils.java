package com.shanthigear.util;

import com.shanthigear.dto.PaymentRequestDTO;
import com.shanthigear.exception.InvalidPaymentException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Currency;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class for payment-related operations.
 * Provides methods for validation, reference generation, and amount calculations.
 */
public final class PaymentUtils {

    private static final String REFERENCE_PREFIX = "PMT";
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
    private static final Set<String> SUPPORTED_CURRENCIES;
    private static final AtomicLong REFERENCE_COUNTER = new AtomicLong(System.currentTimeMillis());
    
    static {
        Set<String> currencies = new HashSet<>();
        currencies.add("USD");
        currencies.add("EUR");
        currencies.add("GBP");
        currencies.add("JPY");
        currencies.add("CAD");
        currencies.add("INR");
        SUPPORTED_CURRENCIES = Collections.unmodifiableSet(currencies);
    }

    private PaymentUtils() {
        // Private constructor to prevent instantiation
    }



    /**
     * Converts a Date to LocalDateTime
     * @param date the date to convert
     * @return LocalDateTime representation of the date
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Converts a LocalDateTime to Date
     * @param localDateTime the LocalDateTime to convert
     * @return Date representation of the LocalDateTime
     */
    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Formats a date using the standard date pattern
     * @param date the date to format
     * @return formatted date string
     */
    public static String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(DATE_PATTERN).format(date);
    }

    /**
     * Validates the payment request DTO.
     *
     * @param paymentRequest the payment request to validate
     * @throws InvalidPaymentException if the payment request is invalid
     */
    public static void validatePaymentRequest(PaymentRequestDTO paymentRequest) {
        if (paymentRequest == null) {
            throw new InvalidPaymentException("Payment request cannot be null");
        }

        validateCurrency(paymentRequest.getCurrency());
        
        if (paymentRequest.getAmount() == null || paymentRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Payment amount must be greater than zero");
        }

        if (paymentRequest.getDueDate() != null && !isValidDate(paymentRequest.getDueDate())) {
            throw new InvalidPaymentException("Invalid due date format. Expected format: " + DATE_PATTERN);
        }
    }

    /**
     * Validates if the currency is supported.
     *
     * @param currencyCode the ISO 4217 currency code to validate
     * @throws InvalidPaymentException if the currency is not supported
     */
    public static void validateCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.length() != 3) {
            throw new InvalidPaymentException("Currency code must be a 3-letter ISO code");
        }

        try {
            Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException e) {
            throw new InvalidPaymentException("Invalid currency code: " + currencyCode, e);
        }

        if (!SUPPORTED_CURRENCIES.contains(currencyCode)) {
            throw new InvalidPaymentException("Unsupported currency: " + currencyCode);
        }
    }

    /**
     * Generates a unique payment reference.
     * Format: PMT-{timestamp}-{counter}
     * 
     * @return a unique payment reference string
     */
    public static String generatePaymentReference() {
        long timestamp = System.currentTimeMillis();
        long counter = REFERENCE_COUNTER.incrementAndGet() % 10000;
        return String.format("%s-%d-%04d", REFERENCE_PREFIX, timestamp, counter);
    }

    /**
     * Calculates the amount in the target currency using the given exchange rate.
     *
     * @param amount the amount in the source currency
     * @param exchangeRate the exchange rate to apply
     * @param targetDecimals the number of decimal places for the target currency
     * @return the converted amount in the target currency
     */
    public static BigDecimal convertAmount(BigDecimal amount, BigDecimal exchangeRate, int targetDecimals) {
        if (amount == null || exchangeRate == null) {
            throw new IllegalArgumentException("Amount and exchange rate must not be null");
        }
        
        return amount.multiply(exchangeRate)
                .setScale(targetDecimals, RoundingMode.HALF_EVEN);
    }

    /**
     * Formats an amount with the specified number of decimal places.
     *
     * @param amount the amount to format
     * @param decimals the number of decimal places
     * @return the formatted amount string
     */
    public static String formatAmount(BigDecimal amount, int decimals) {
        if (amount == null) {
            return "";
        }
        return amount.setScale(decimals, RoundingMode.HALF_EVEN).toPlainString();
    }

    /**
     * Checks if a date string is valid and not in the past.
     *
     * @param dateStr the date string to check (format: yyyy-MM-dd)
     * @return true if the date is valid and not in the past, false otherwise
     */
    public static boolean isValidFutureDate(String dateStr) {
        if (!isValidDate(dateStr)) {
            return false;
        }
        LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
        return !date.isBefore(LocalDate.now());
    }

    /**
     * Validates if a date string has the correct format.
     *
     * @param dateStr the date string to validate
     * @return true if the date string is valid, false otherwise
     */
    private static boolean isValidDate(String dateStr) {
        if (dateStr == null) {
            return false;
        }
        try {
            LocalDate.parse(dateStr, DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
