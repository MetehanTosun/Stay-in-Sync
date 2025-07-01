package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.generell_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.ConstantNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EqualsOperator implements Operation {


    /**
     * Validates the inputs for the EQUALS operation.
     * <p>
     * This operation requires at least two inputs to compare. To ensure that the
     * comparison is meaningful and not static, it enforces that a maximum of one
     * input can be a {@link ConstantNode}.
     * <p>
     * This allows for flexible comparisons
     * while preventing redundant static comparisons (constant vs. constant).
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the configuration is invalid.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();

        if (inputs == null || inputs.size() < 2) {
            throw new IllegalArgumentException(
                    "EQUALS operation for node '" + node.getNodeName() + "' requires at least 2 inputs to compare."
            );
        }

        int constantNodeCount = 0;
        for (InputNode input : inputs) {
            if (input instanceof ConstantNode) {
                constantNodeCount++;
            }
        }

        if (constantNodeCount > 1) {
            throw new IllegalArgumentException(
                    "EQUALS operation for node '" + node.getNodeName() + "' is invalid. A maximum of one ConstantNode is allowed as an input. Found: " + constantNodeCount
            );
        }
    }


    /**
     * Executes the value comparison for two or more operands.
     * It returns true if and only if all input values are equal to each other.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if all values are equal, otherwise {@code false}.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<InputNode> inputs = node.getInputProviders();
        Object referenceValue;
        try {
            referenceValue = inputs.get(0).getValue(dataContext);
        } catch (IllegalStateException e) {
            return false;
        }

        for (int i = 1; i < inputs.size(); i++) {
            Object currentValue;
            try {
                currentValue = inputs.get(i).getValue(dataContext);
            } catch (IllegalStateException e) {
                return false;
            }

            if (!Objects.equals(referenceValue, currentValue)) {
                return false;
            }
        }

        return true;
    }
}
