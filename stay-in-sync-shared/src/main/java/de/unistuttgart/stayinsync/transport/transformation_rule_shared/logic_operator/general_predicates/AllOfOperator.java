package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.exception.OperatorValidationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;

import java.util.List;
import java.util.Map;

public class AllOfOperator implements Operation {

    /**
     * Validates that the LogicNode is correctly configured for the ALL_OF operation.
     * <p>
     * This operation requires at least one input node to evaluate.
     *
     * @param node The LogicNode to validate.
     * @throws OperatorValidationException if the node has no inputs.
     */
    @Override
    public void validateNode(LogicNode node)throws OperatorValidationException {
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.isEmpty()) {
            throw new OperatorValidationException(
                    "ALL_OF operation for node '" + node.getName() + "' requires at least 1 input."
            );
        }
    }

    /**
     * Executes the strict "all must be true" check.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} only if all inputs have a calculated value of {@code Boolean.TRUE},
     * otherwise returns {@code false}.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<Node> inputs = node.getInputNodes();

        for (Node inputNode : inputs) {
            Object value = inputNode.getCalculatedResult();

            // Strict check: The value must be exactly Boolean.TRUE.
            // The Boolean.TRUE.equals() handles null, false, and non-boolean types correctly,
            // returning false for all of them.
            if (!Boolean.TRUE.equals(value)) {
                // If any value is not exactly Boolean.TRUE, the condition is immediately violated.
                return false;
            }
        }

        // If the loop completes, it means all values were exactly Boolean.TRUE.
        return true;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}