package de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.generell_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.ConstantNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.Operation;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NotEqualsOperator implements Operation {

    /**
     * Validates the inputs for the NOT_EQUALS operation.
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
     * Executes the inequality comparison for two or more operands.
     * <p>
     * It returns {@code true} as soon as it finds a value that is different from
     * the first value in the input list. If all values are identical, it returns
     * {@code false}.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if any value differs, otherwise {@code false}.
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
                return true;
            }
        }

        return false;
    }
}
