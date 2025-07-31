package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.exception.OperatorValidationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.ConstantNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;

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
     * @throws OperatorValidationException if the configuration is invalid.
     */
    @Override
    public void validateNode(LogicNode node)throws OperatorValidationException {
        List<Node> inputs = node.getInputNodes();

        if (inputs == null || inputs.size() < 2) {
            throw new OperatorValidationException(
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
            throw new OperatorValidationException(
                    "EQUALS operation for node '" + node.getName() + "' is invalid. A maximum of one ConstantNode is allowed as an input. Found: " + constantNodeCount
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
        List<Node> inputs = node.getInputNodes();

        Object referenceValue = inputs.get(0).getCalculatedResult();

        for (int i = 1; i < inputs.size(); i++) {
            Object currentValue = inputs.get(i).getCalculatedResult();

            // If any value is not equal, the condition is immediately true.
            // Objects.equals is null-safe and handles all comparisons correctly.
            if (!Objects.equals(referenceValue, currentValue)) {
                return true;
            }
        }

        // If the loop completes, all values were identical.
        return false;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}
