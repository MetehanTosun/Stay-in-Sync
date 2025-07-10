package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.number_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.ConstantNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.Node;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;

import java.util.List;
import java.util.Map;

public class BetweenOperator implements Operation {

    /**
     * Validates that the node is configured correctly for the BETWEEN operation.
     * <p>
     * It checks for two conditions:
     * <ol>
     * <li>The node must have exactly three inputs (value, lower bound, upper bound).</li>
     * <li>It is not permitted for all three inputs to be {@link ConstantNode}s,
     * as this would make the node's result static.</li>
     * </ol>
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the validation fails.
     */
    @Override
    public void validateNode(LogicNode node) {
        List<Node> inputs = node.getInputNodes();

        // Rule 1: Must have exactly 3 inputs
        if (inputs == null || inputs.size() != 3) {
            throw new IllegalArgumentException(
                    "BETWEEN operation for node '" + node.getName() + "' requires exactly 3 inputs: the value to check, the lower bound, and the upper bound."
            );
        }

        // Rule 2: Not all inputs can be constants
        long constantNodeCount = inputs.stream().filter(input -> input instanceof ConstantNode).count();

        if (constantNodeCount == 3) {
            throw new IllegalArgumentException(
                    "BETWEEN operation for node '" + node.getName() + "' is invalid: all three inputs are constants. This should be simplified to a single ConstantNode(true/false)."
            );
        }
    }

    /**
     * Executes the between-check.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the first input value is >= the second and <= the third.
     * Returns {@code false} if any provided value is null or not a number.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<Node> inputs = node.getInputNodes();

        Object valueToCheck = inputs.get(0).getCalculatedResult();
        Object lowerBound = inputs.get(1).getCalculatedResult();
        Object upperBound = inputs.get(2).getCalculatedResult();

        // All three provided values must be numbers to be comparable.
        // The 'instanceof' check correctly handles null values.
        if (valueToCheck instanceof Number && lowerBound instanceof Number && upperBound instanceof Number) {
            Number numberToCheck = (Number) valueToCheck;
            Number numberLower = (Number) lowerBound;
            Number numberUpper = (Number) upperBound;

            // Perform the inclusive check.
            double val = numberToCheck.doubleValue();
            return val >= numberLower.doubleValue() && val <= numberUpper.doubleValue();
        }

        // If types are not all numeric or a value was null, the predicate is false.
        return false;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}