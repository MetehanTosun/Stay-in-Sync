package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.number_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;

import java.util.List;
import java.util.Map;

/**
 * An abstract base class for numeric comparison operations that take two inputs.
 * It handles the common logic of input validation, value extraction, and type checking.
 * Subclasses only need to implement the specific comparison logic.
 */
public abstract class AbstractNumericComparisonOperator implements Operation {

    /**
     * Validates that the node is configured with exactly two inputs.
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node does not have exactly two inputs.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();
        if (inputs == null || inputs.size() != 2) {
            throw new IllegalArgumentException(
                    "Numeric comparison for node '" + node.getNodeName() + "' requires exactly 2 inputs: the value and the threshold."
            );
        }
    }

    /**
     * Executes the comparison logic.
     * <p>
     * It retrieves the two input values, checks if they are both instances of {@link Number},
     * and if so, delegates the actual comparison to the abstract {@link #compare(Number, Number)} method.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the comparison is successful, otherwise {@code false}. Returns {@code false}
     *         if any input value cannot be retrieved or if any value is not a number.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<InputNode> inputs = node.getInputProviders();
        Object value1, value2;

        // Safely retrieve both input values. If a value is missing (e.g., JSON path not found),
        // getValue() throws an IllegalStateException. We catch it and return false, as a
        // comparison with a missing value cannot be true.
        try {
            value1 = inputs.get(0).getValue(dataContext);
            value2 = inputs.get(1).getValue(dataContext);
        } catch (IllegalStateException e) {
            return false; // Comparison is not possible if a value is missing.
        }

        // Both values must be numbers to be comparable.
        if (value1 instanceof Number && value2 instanceof Number) {
            // Delegate the actual comparison to the concrete implementation.
            return compare((Number) value1, (Number) value2);
        }

        // If types are not numeric, the comparison predicate is false.
        return false;
    }

    /**
     * Performs the specific numeric comparison between two numbers.
     * Subclasses must implement this method to define their behavior (e.g., >, <, ==).
     *
     * @param number1 The first number in the comparison (from the first input).
     * @param number2 The second number in the comparison (from the second input).
     * @return The boolean result of the comparison.
     */
    protected abstract boolean compare(Number number1, Number number2);
}
