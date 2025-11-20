package com.shanthigear.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

/**
 * Mapper for converting between different date and time types.
 * Used by MapStruct for date conversions between entity and DTO objects.
 */
@Mapper(componentModel = "spring")
@Component
@SuppressWarnings("unused")
public interface DateMapper {
    
    /**
     * Converts a Date to a LocalDateTime in the system's default timezone.
     *
     * @param date the Date to convert, can be null
     * @return the corresponding LocalDateTime, or null if the input is null
     */
    @Named("toLocalDateTime")
    default LocalDateTime asLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
    
    /**
     * Converts a LocalDateTime to a Date in the system's default timezone.
     *
     * @param localDateTime the LocalDateTime to convert, can be null
     * @return the corresponding Date, or null if the input is null
     */
    @Named("toDate")
    default Date asDate(LocalDateTime localDateTime) {
        return localDateTime != null ? Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()) : null;
    }
    
    /**
     * Converts a Date to a LocalDate in the system's default timezone.
     *
     * @param date the Date to convert, can be null
     * @return the corresponding LocalDate, or null if the input is null
     */
    @Named("toLocalDate")
    default LocalDate asLocalDate(Date date) {
        return date != null ? date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;
    }
    
    /**
     * Converts a LocalDate to a Date in the system's default timezone.
     *
     * @param localDate the LocalDate to convert, can be null
     * @return the corresponding Date, or null if the input is null
     */
    @Named("toDateFromLocalDate")
    default Date asDateFromLocalDate(LocalDate localDate) {
        return localDate != null ? Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
    }
    
    /**
     * Converts milliseconds since epoch to LocalDateTime.
     * @param epochMilli the milliseconds since epoch, can be null
     * @return the corresponding LocalDateTime, or null if the input is null
     */
    @Named("epochMilliToLocalDateTime")
    default LocalDateTime fromEpochMilli(Long epochMilli) {
        return epochMilli != null ? new Date(epochMilli).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null;
    }
    
    /**
     * Converts LocalDateTime to milliseconds since epoch.
     * @param localDateTime the LocalDateTime to convert, can be null
     * @return the milliseconds since epoch, or null if the input is null
     */
    @Named("toEpochMilli")
    default Long toEpochMilli(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }
    
    /**
     * Converts a timestamp in milliseconds to a LocalDateTime in the system's default timezone.
     *
     * @param timestamp the timestamp in milliseconds since epoch
     * @return the corresponding LocalDateTime, or null if the input is null
     */
    @Named("toLocalDateTimeFromTimestamp")
    default LocalDateTime asLocalDateTimeFromTimestamp(Long timestamp) {
        return timestamp != null ? new Date(timestamp).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null;
    }
    
    /**
     * Converts a LocalDateTime to a timestamp in milliseconds since epoch.
     *
     * @param localDateTime the LocalDateTime to convert, can be null
     * @return the corresponding timestamp in milliseconds, or null if the input is null
     */
    @Named("toTimestamp")
    default Long asTimestamp(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }
}
