package de.unistuttgart.graphengine.logic_operator.datetime_predicates;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;

public class TimezoneOffsetEqualsOperator extends AbstractDateTimeNumericOperator {

    /**
     * Checks if the date-time's timezone offset in minutes matches a given number.
     * @param dateTime The date-time object to check.
     * @param comparisonValue The expected offset in minutes (e.g., 120 for CEST, 0 for UTC).
     * @return {@code true} if the timezone offset matches.
     */
    @Override
    protected boolean compareDateTimeAndNumber(ZonedDateTime dateTime, int comparisonValue) {
        ZoneOffset offset = dateTime.getOffset();
        int offsetInMinutes = offset.getTotalSeconds() / 60;

        return offsetInMinutes == comparisonValue;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}