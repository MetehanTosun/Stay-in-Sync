package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.datetime_predicates;

import java.time.ZonedDateTime;

public class WeekdayIsOperator extends AbstractDateTimeNumericOperator {

    /**
     * Checks if the date-time's day of the week matches a given number.
     * The convention is Monday=1, Tuesday=2, ..., Sunday=7.
     * @param dateTime The date-time object.
     * @param comparisonValue The numeric value of the weekday (1-7).
     * @return {@code true} if the weekday matches.
     */
    @Override
    protected boolean compareDateTimeAndNumber(ZonedDateTime dateTime, int comparisonValue) {
        // ZonedDateTime.getDayOfWeek().getValue() returns an int from 1 (Monday) to 7 (Sunday).
        return dateTime.getDayOfWeek().getValue() == comparisonValue;
    }
}