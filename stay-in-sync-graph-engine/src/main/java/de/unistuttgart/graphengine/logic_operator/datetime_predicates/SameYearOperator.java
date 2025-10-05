package de.unistuttgart.graphengine.logic_operator.datetime_predicates;

import java.time.ZonedDateTime;

public class SameYearOperator extends AbstractTwoDateTimeOperator {

    /**
     * Checks if two date-times occur in the same year.
     * @param dt1 The first date-time object.
     * @param dt2 The second date-time object.
     * @return {@code true} if both date-times are in the same year.
     */
    @Override
    protected boolean compareDateTimes(ZonedDateTime dt1, ZonedDateTime dt2) {
        return dt1.getYear() == dt2.getYear();
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}