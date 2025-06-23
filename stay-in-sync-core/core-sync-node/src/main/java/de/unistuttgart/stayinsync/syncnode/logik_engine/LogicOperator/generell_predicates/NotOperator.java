package de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.generell_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.Operation;

import java.util.List;
import java.util.Map;

public class NotOperator implements Operation {

    /**
     * Validates that the LogicNode is correctly configured for the NOT operation.
     * <p>
     * This operation requires exactly one input.
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node does not have exactly one input.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();
        if (inputs == null || inputs.size() != 1) {
            throw new IllegalArgumentException(
                    "NOT operation for node '" + node.getNodeName() + "' requires exactly 1 input, but got " + (inputs == null ? 0 : inputs.size())
            );
        }
    }

    /**
     * Executes the logical negation.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return The negated boolean value of the input.
     * @throws IllegalArgumentException if the resolved input value is not a Boolean.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        InputNode inputProvider = node.getInputProviders().get(0);

        Object value;
        try {
            value = inputProvider.getValue(dataContext);
        } catch (IllegalStateException e) {
            throw new IllegalStateException(
                    "Failed to resolve the input value for NOT operation in node '" + node.getNodeName() + "'.", e
            );
        }

        // Runtime type validation: The Value must be a boolean.
        if (!(value instanceof Boolean)) {
            throw new IllegalArgumentException(
                    "NOT operation for node '" + node.getNodeName() + "' requires a boolean input, but got a " + (value == null ? "null" : value.getClass().getSimpleName())
            );
        }

        return !((Boolean) value);
    }
}
