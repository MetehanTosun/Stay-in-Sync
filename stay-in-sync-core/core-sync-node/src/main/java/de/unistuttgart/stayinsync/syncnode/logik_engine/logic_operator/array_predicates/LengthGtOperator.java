package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.array_predicates;

public class LengthGtOperator extends AbstractArrayLengthComparisonOperator {

    /**
     * Compares the actual length to the expected length.
     *
     * @param actualLength   The determined length of the array or collection.
     * @param expectedLength The target length to compare against.
     * @return {@code true} if {@code actualLength > expectedLength}, otherwise {@code false}.
     */
    @Override
    protected boolean compare(int actualLength, int expectedLength) {
        return actualLength > expectedLength;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}
