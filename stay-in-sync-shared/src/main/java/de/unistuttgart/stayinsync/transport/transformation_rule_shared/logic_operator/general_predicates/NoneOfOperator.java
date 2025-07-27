package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;

import java.util.List;
import java.util.Map;

public class NoneOfOperator implements Operation {

    /**
     * Validates that the LogicNode is correctly configured for the NONE_OF operation.
     * <p>
     * This operation requires at least one input node to evaluate.
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node has no inputs.
     */
    @Override
    public void validateNode(LogicNode node) {
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException(
                    "NONE_OF operation for node '" + node.getName() + "' requires at least 1 input."
            );
        }
    }

    /**
     * Executes the strict "none are true" check.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} only if all inputs have a calculated value other than
     * {@code Boolean.TRUE}, otherwise returns {@code false}.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<Node> inputs = node.getInputNodes();

        for (Node inputNode : inputs) {
            Object value = inputNode.getCalculatedResult();

            // Strict check: The value must be exactly Boolean.TRUE to fail the condition.
            // The Boolean.TRUE.equals() handles null, false, and non-boolean types correctly.
            if (Boolean.TRUE.equals(value)) {
                // If any value is exactly Boolean.TRUE, the "none of" condition is immediately violated.
                return false;
            }
        }

        // If the loop completes, it means none of the values were exactly Boolean.TRUE.
        return true;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}