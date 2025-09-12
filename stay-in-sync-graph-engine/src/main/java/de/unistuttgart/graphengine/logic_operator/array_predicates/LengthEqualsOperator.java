package de.unistuttgart.graphengine.logic_operator.array_predicates;

public class LengthEqualsOperator extends AbstractArrayLengthComparisonOperator {

    /**
     * Compares the actual length to the expected length for equality.
     *
     * @param actualLength   The determined length of the array or collection.
     * @param expectedLength The target length to compare against.
     * @return {@code true} if {@code actualLength == expectedLength}, otherwise {@code false}.
     */
    @Override
    protected boolean compare(int actualLength, int expectedLength) {

        return actualLength == expectedLength;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}