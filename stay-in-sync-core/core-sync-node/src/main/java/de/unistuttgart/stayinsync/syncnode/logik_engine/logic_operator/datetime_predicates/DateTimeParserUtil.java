package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.datetime_predicates;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

public class DateTimeParserUtil {

    /**
     * Converts an object into a ZonedDateTime, if possible.
     * Assumes ISO 8601 format for strings.
     *
     * @param obj The object to convert.
     * @return A ZonedDateTime instance, or null if conversion is not possible.
     */
    public static ZonedDateTime toZonedDateTime(Object obj) {
        if (obj == null) return null;
        if (obj instanceof TemporalAccessor) {
            return ZonedDateTime.from((TemporalAccessor) obj);
        }
        if (obj instanceof String) {
            try {
                return ZonedDateTime.parse((String) obj);
            } catch (DateTimeParseException e) {
                return null; // As per our decision, only ISO-8601 is supported.
            }
        }
        return null;
    }
}