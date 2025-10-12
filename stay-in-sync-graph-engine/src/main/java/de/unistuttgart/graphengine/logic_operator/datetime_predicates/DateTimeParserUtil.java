package de.unistuttgart.graphengine.logic_operator.datetime_predicates;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

public class DateTimeParserUtil {

    /**
     * Converts an object into a ZonedDateTime, if possible.
     * Supports both full ISO 8601 format (e.g., "2025-10-02T14:30:00Z")
     * and date-only format (e.g., "2025-10-02").
     * Date-only inputs are converted to midnight UTC.
     *
     * @param obj The object to convert.
     * @return A ZonedDateTime instance, or null if conversion is not possible.
     */
    public static ZonedDateTime toZonedDateTime(Object obj) {
        if (obj == null) return null;

        // Handle LocalDate separately before general TemporalAccessor check
        if (obj instanceof LocalDate) {
            return ((LocalDate) obj).atStartOfDay(ZoneOffset.UTC);
        }

        if (obj instanceof TemporalAccessor) {
            try {
                return ZonedDateTime.from((TemporalAccessor) obj);
            } catch (DateTimeException e) {
                // Cannot convert this TemporalAccessor to ZonedDateTime
                return null;
            }
        }

        if (obj instanceof String) {
            String str = (String) obj;
            try {
                // Try parsing as full ZonedDateTime first
                return ZonedDateTime.parse(str);
            } catch (DateTimeParseException e) {
                try {
                    // If only date is provided, convert to midnight UTC
                    LocalDate date = LocalDate.parse(str);
                    return date.atStartOfDay(ZoneOffset.UTC);
                } catch (DateTimeParseException e2) {
                    return null; // Invalid format
                }
            }
        }
        return null;
    }
}