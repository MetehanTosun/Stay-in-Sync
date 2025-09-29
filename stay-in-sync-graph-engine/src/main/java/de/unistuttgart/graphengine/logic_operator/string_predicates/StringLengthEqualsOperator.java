package de.unistuttgart.graphengine.logic_operator.string_predicates;

public class StringLengthEqualsOperator extends AbstractStringLengthComparisonOperator {

    /**
     * Checks if the string's length is equal to the given numeric value.
     * @param length The actual length of the input string.
     * @param value  The numeric value to compare against.
     * @return {@code true} if the length is exactly equal to the value.
     */
    @Override
    protected boolean compareLength(int length, int value) {
        return length == value;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}