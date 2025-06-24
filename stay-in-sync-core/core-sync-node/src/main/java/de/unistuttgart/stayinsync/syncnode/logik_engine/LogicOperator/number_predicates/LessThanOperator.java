package de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.number_predicates;

public class LessThanOperator extends AbstractNumericComparisonOperator {

    /**
     * Compares two numbers to determine if the first is strictly less than the second.
     *
     * @param number1 The first number.
     * @param number2 The second number.
     * @return {@code true} if {@code number1 < number2}, otherwise {@code false}.
     */
    @Override
    protected boolean compare(Number number1, Number number2) {
        // Re-using the logic from the abstract class, only the comparison operator changes.
        return number1.doubleValue() < number2.doubleValue();
    }
}
