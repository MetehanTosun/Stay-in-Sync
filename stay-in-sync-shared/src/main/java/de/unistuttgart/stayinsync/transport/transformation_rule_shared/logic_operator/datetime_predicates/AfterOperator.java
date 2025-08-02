package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.datetime_predicates;

import java.time.ZonedDateTime;

public class AfterOperator extends AbstractTwoDateTimeOperator {

    /**
     * Checks if the first date-time is strictly after the second date-time.
     * @param dt1 The first date-time object.
     * @param dt2 The second date-time object.
     * @return {@code true} if dt1 is after dt2.
     */
    @Override
    protected boolean compareDateTimes(ZonedDateTime dt1, ZonedDateTime dt2) {
        return dt1.isAfter(dt2);
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}