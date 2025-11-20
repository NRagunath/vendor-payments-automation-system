package com.shanthigear.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

/**
 * Utility class for date and time operations.
 * Provides methods for parsing, formatting, and manipulating dates and times.
 */
@Slf4j
@UtilityClass
public class DateTimeUtil {

    // Common date and time formatters
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String ISO_8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String TIME_FORMAT_12H = "hh:mm a";
    public static final String TIME_FORMAT_24H = "HH:mm";
    
    // Common time zones
    public static final ZoneId UTC = ZoneId.of("UTC");
    public static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
    private static final DateTimeFormatter ISO_8601_FORMATTER = DateTimeFormatter.ofPattern(ISO_8601_FORMAT)
            .withZone(ZoneOffset.UTC);
    
    /**
     * Gets the current date in the system default time zone.
     *
     * @return The current local date
     */
    public static LocalDate nowDate() {
        return LocalDate.now();
    }
    
    /**
     * Gets the current date and time in the system default time zone.
     *
     * @return The current local date and time
     */
    public static LocalDateTime nowDateTime() {
        return LocalDateTime.now();
    }
    
    /**
     * Gets the current date and time in UTC.
     *
     * @return The current date and time in UTC
     */
    public static ZonedDateTime nowUtc() {
        return ZonedDateTime.now(UTC);
    }
    
    /**
     * Converts a LocalDate to a formatted string.
     *
     * @param date The date to format
     * @return The formatted date string
     */
    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : null;
    }
    
    /**
     * Converts a LocalDateTime to a formatted string.
     *
     * @param dateTime The date and time to format
     * @return The formatted date and time string
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : null;
    }
    
    /**
     * Formats a ZonedDateTime to an ISO 8601 string in UTC.
     *
     * @param zonedDateTime The zoned date time to format
     * @return The ISO 8601 formatted string
     */
    public static String formatIso8601(ZonedDateTime zonedDateTime) {
        return zonedDateTime != null ? zonedDateTime.format(ISO_8601_FORMATTER) : null;
    }
    
    /**
     * Parses a date string to a LocalDate.
     *
     * @param dateString The date string to parse
     * @return The parsed LocalDate
     * @throws DateTimeParseException if the text cannot be parsed
     */
    public static LocalDate parseDate(String dateString) {
        return dateString != null ? LocalDate.parse(dateString, DATE_FORMATTER) : null;
    }
    
    /**
     * Parses a date-time string to a LocalDateTime.
     *
     * @param dateTimeString The date-time string to parse
     * @return The parsed LocalDateTime
     * @throws DateTimeParseException if the text cannot be parsed
     */
    public static LocalDateTime parseDateTime(String dateTimeString) {
        return dateTimeString != null ? LocalDateTime.parse(dateTimeString, DATE_TIME_FORMATTER) : null;
    }
    
    /**
     * Converts a java.util.Date to a LocalDateTime in the system default time zone.
     *
     * @param date The Date to convert
     * @return The converted LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        return date != null ? date.toInstant().atZone(SYSTEM_ZONE).toLocalDateTime() : null;
    }
    
    /**
     * Converts a LocalDateTime to a java.util.Date.
     *
     * @param localDateTime The LocalDateTime to convert
     * @return The converted Date
     */
    public static Date toDate(LocalDateTime localDateTime) {
        return localDateTime != null ? Date.from(localDateTime.atZone(SYSTEM_ZONE).toInstant()) : null;
    }
    
    /**
     * Converts a LocalDate to a java.util.Date at the start of the day.
     *
     * @param localDate The LocalDate to convert
     * @return The converted Date at start of day
     */
    public static Date toDate(LocalDate localDate) {
        return localDate != null ? Date.from(localDate.atStartOfDay(SYSTEM_ZONE).toInstant()) : null;
    }
    
    /**
     * Gets the start of the day for a given date in the system time zone.
     *
     * @param date The reference date
     * @return The start of the day
     */
    public static LocalDateTime startOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }
    
    /**
     * Gets the end of the day for a given date in the system time zone.
     *
     * @param date The reference date
     * @return The end of the day (last nanosecond)
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        return date != null ? date.atTime(23, 59, 59, 999_999_999) : null;
    }
    
    /**
     * Gets the start of the current month.
     *
     * @return The first day of the current month at 00:00:00
     */
    public static LocalDateTime startOfCurrentMonth() {
        return LocalDate.now().withDayOfMonth(1).atStartOfDay();
    }
    
    /**
     * Gets the end of the current month.
     *
     * @return The last day of the current month at 23:59:59.999999999
     */
    public static LocalDateTime endOfCurrentMonth() {
        return LocalDate.now().with(TemporalAdjusters.lastDayOfMonth())
                .atTime(23, 59, 59, 999_999_999);
    }
    
    /**
     * Calculates the difference in days between two dates.
     *
     * @param startDate The start date
     * @param endDate The end date
     * @return The number of days between the dates
     */
    public static long daysBetween(LocalDate startDate, LocalDate endDate) {
        return ChronoUnit.DAYS.between(startDate, endDate);
    }
    
    /**
     * Adds a number of business days to a date, skipping weekends.
     *
     * @param date The start date
     * @param businessDays The number of business days to add (can be negative)
     * @return The resulting date
     */
    public static LocalDate addBusinessDays(LocalDate date, int businessDays) {
        if (date == null) {
            return null;
        }
        
        LocalDate result = date;
        int step = businessDays < 0 ? -1 : 1;
        
        while (businessDays != 0) {
            result = result.plusDays(step);
            
            // Skip weekends
            if (result.getDayOfWeek() != DayOfWeek.SATURDAY && 
                result.getDayOfWeek() != DayOfWeek.SUNDAY) {
                businessDays -= step;
            }
        }
        
        return result;
    }
    
    /**
     * Checks if a date is a business day (Monday to Friday).
     *
     * @param date The date to check
     * @return true if the date is a business day, false otherwise
     */
    public static boolean isBusinessDay(LocalDate date) {
        return date != null && 
               date.getDayOfWeek() != DayOfWeek.SATURDAY && 
               date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }
    
    /**
     * Gets the next business day from a given date.
     *
     * @param date The reference date
     * @return The next business day
     */
    public static LocalDate nextBusinessDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        
        LocalDate nextDay = date.plusDays(1);
        while (!isBusinessDay(nextDay)) {
            nextDay = nextDay.plusDays(1);
        }
        return nextDay;
    }
    
    /**
     * Converts a LocalDateTime from one time zone to another.
     *
     * @param dateTime The date and time to convert
     * @param fromZone The source time zone
     * @param toZone The target time zone
     * @return The converted LocalDateTime in the target time zone
     */
    public static LocalDateTime convertTimeZone(
            LocalDateTime dateTime, ZoneId fromZone, ZoneId toZone) {
        return dateTime != null ? 
               dateTime.atZone(fromZone).withZoneSameInstant(toZone).toLocalDateTime() : null;
    }
    
    /**
     * Gets the current time in milliseconds since the Unix epoch.
     *
     * @return The current time in milliseconds
     */
    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }
    
    /**
     * Gets the current time in seconds since the Unix epoch.
     *
     * @return The current time in seconds
     */
    public static long currentTimeSeconds() {
        return System.currentTimeMillis() / 1000L;
    }
}
