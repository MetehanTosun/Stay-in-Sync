package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.string_predicates;

public class StringLengthLtOperator extends AbstractStringLengthComparisonOperator {

    /**
     * Checks if the string's length is less than the given numeric value.
     * @param length The actual length of the input string.
     * @param value  The numeric value to compare against.
     * @return {@code true} if the length is less than the value.
     */
    @Override
    protected boolean compareLength(int length, int value) {
        return length < value;
    }
}