package de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.number_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.ConstantNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.Operation;

import java.util.List;
import java.util.Map;

public class NotBetweenOperator implements Operation {

    /**
     * Validates that the node is configured correctly for the NOT_BETWEEN operation.
     * <p>
     * It checks for two conditions:
     * <ol>
     *     <li>The node must have exactly three inputs (value, lower bound, upper bound).</li>
     *     <li>It is not permitted for all three inputs to be {@link ConstantNode}s,
     *         as this would make the node's result static and better represented by a single ConstantNode.</li>
     * </ol>
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the validation fails.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();

        // Rule 1: Must have exactly 3 inputs
        if (inputs == null || inputs.size() != 3) {
            throw new IllegalArgumentException(
                    "NOT_BETWEEN operation for node '" + node.getNodeName() + "' requires exactly 3 inputs: the value to check, the lower bound, and the upper bound."
            );
        }

        // Rule 2: Not all inputs can be constants
        int constantNodeCount = 0;
        for (InputNode input : inputs) {
            if (input.isConstantNode()) {
                constantNodeCount++;
            }
        }

        if (constantNodeCount == 3) {
            throw new IllegalArgumentException(
                    "NOT_BETWEEN operation for node '" + node.getNodeName() + "' is invalid: all three inputs are constants. The result of this node is fixed and it should be simplified to a single ConstantNode(true/false) in the graph definition."
            );
        }
    }

    /**
     * Executes the not-between-check.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the first input value is < the second or > the third.
     *         Returns {@code false} if any input value cannot be retrieved or if any value is not a number.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<InputNode> inputs = node.getInputProviders();
        Object valueToCheck, lowerBound, upperBound;

        try {
            valueToCheck = inputs.get(0).getValue(dataContext);
            lowerBound = inputs.get(1).getValue(dataContext);
            upperBound = inputs.get(2).getValue(dataContext);
        } catch (IllegalStateException e) {
            return false;
        }

        if (valueToCheck instanceof Number && lowerBound instanceof Number && upperBound instanceof Number) {
            Number numberToCheck = (Number) valueToCheck;
            Number numberLower = (Number) lowerBound;
            Number numberUpper = (Number) upperBound;

            // Perform the "not between" check.
            double val = numberToCheck.doubleValue();
            return val < numberLower.doubleValue() || val > numberUpper.doubleValue();
        }

        return false;
    }
}
