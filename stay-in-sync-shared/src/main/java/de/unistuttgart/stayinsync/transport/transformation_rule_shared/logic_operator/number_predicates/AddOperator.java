package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.number_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.exception.OperatorValidationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;

import java.util.List;
import java.util.Map;

public class AddOperator implements Operation {

    /**
     * Validates that the node is correctly configured for the ADD operation.
     * <p>
     * This operation requires at least two inputs to perform an addition.
     *
     * @param node The LogicNode to validate.
     * @throws OperatorValidationException if the node does not have at least two inputs.
     */
    @Override
    public void validateNode(LogicNode node) {
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.size() < 2) {
            throw new OperatorValidationException(
                    "ADD operation for node '" + node.getName() + "' requires at least 2 inputs."
            );
        }
    }

    /**
     * Executes the addition of all input values.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return A {@link Double} representing the sum of all numeric inputs.
     * @throws IllegalArgumentException if any provided input value is not a Number.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<Node> inputs = node.getInputNodes();
        double sum = 0.0;

        for (Node inputNode : inputs) {
            // Get the pre-calculated value from the parent node.
            Object value = inputNode.getCalculatedResult();

            // --- RUNTIME TYPE VALIDATION ---
            // Ensure every input is a number before adding it.
            if (!(value instanceof Number)) {
                throw new IllegalArgumentException(
                        "ADD operation for node '" + node.getName() + "' requires numeric inputs, but got a " + (value == null ? "null" : value.getClass().getSimpleName())
                );
            }

            sum += ((Number) value).doubleValue();
        }

        return sum;
    }

    @Override
    public Class<?> getReturnType(){
        return Double.class;
    }
}