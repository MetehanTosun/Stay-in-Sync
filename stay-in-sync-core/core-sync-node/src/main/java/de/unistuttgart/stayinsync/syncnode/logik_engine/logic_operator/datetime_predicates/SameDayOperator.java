package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.datetime_predicates;

import java.time.ZonedDateTime;

public class SameDayOperator extends AbstractTwoDateTimeOperator {

    /**
     * Checks if two date-times occur on the same calendar day, considering their timezones.
     * @param dt1 The first date-time object.
     * @param dt2 The second date-time object.
     * @return {@code true} if both date-times represent the same calendar day.
     */
    @Override
    protected boolean compareDateTimes(ZonedDateTime dt1, ZonedDateTime dt2) {
        // toLocalDate() correctly converts the ZonedDateTime to the calendar date in its timezone.
        return dt1.toLocalDate().equals(dt2.toLocalDate());
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}