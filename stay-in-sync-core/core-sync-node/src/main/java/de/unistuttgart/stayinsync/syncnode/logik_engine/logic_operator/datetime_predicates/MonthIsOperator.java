package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.datetime_predicates;

import java.time.ZonedDateTime;

public class MonthIsOperator extends AbstractDateTimeNumericOperator {

    /**
     * Checks if the date-time's month matches a given number.
     * @param dateTime The date-time object.
     * @param comparisonValue The numeric value of the month (1-12).
     * @return {@code true} if the month matches.
     */
    @Override
    protected boolean compareDateTimeAndNumber(ZonedDateTime dateTime, int comparisonValue) {
        // ZonedDateTime.getMonthValue() returns an int from 1 (January) to 12 (December).
        return dateTime.getMonthValue() == comparisonValue;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}