package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.ConstantNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;

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
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the configuration is invalid.
     */
    public void validateNode(LogicNode node) {
        List<Node> inputs = node.getInputNodes();

        if (inputs == null || inputs.size() < 2) {
            throw new IllegalArgumentException(
                    "EQUALS operation for node '" + node.getName() + "' requires at least 2 inputs to compare."
            );
        }

        int constantNodeCount = 0;
        for (Node input : inputs) {
            if (input instanceof ConstantNode) {
                constantNodeCount++;
            }
        }

        if (constantNodeCount > 1) {
            throw new IllegalArgumentException(
                    "EQUALS operation for node '" + node.getName() + "' is invalid. A maximum of one ConstantNode is allowed as an input. Found: " + constantNodeCount
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
        List<Node> inputs = node.getInputNodes();

        Object referenceValue = inputs.get(0).getCalculatedResult();

        for (int i = 1; i < inputs.size(); i++) {
            Object currentValue = inputs.get(i).getCalculatedResult();

            if (!Objects.equals(referenceValue, currentValue)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}