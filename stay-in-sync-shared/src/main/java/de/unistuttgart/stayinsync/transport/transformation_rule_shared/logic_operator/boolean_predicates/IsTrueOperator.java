package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.boolean_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.exception.OperatorValidationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;


import java.util.List;
import java.util.Map;

public class IsTrueOperator implements Operation {

    /**
     * Validates that the node has at least one input.
     * @param node The LogicNode to validate.
     * @throws OperatorValidationException if the node has no inputs.
     */
    @Override
    public void validateNode(LogicNode node) {
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.isEmpty()) {
            throw new OperatorValidationException("IS_TRUE operation requires at least 1 input.");
        }
    }

    /**
     * Checks if all provided input values are strictly Boolean.TRUE.
     * This acts as an AND-conjunction for the isTrue check.
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if all inputs have a calculated result of Boolean.TRUE, {@code false} otherwise.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<Node> inputs = node.getInputNodes();

        for (Node inputNode : inputs) {
            // Get the pre-calculated result from the parent node.
            Object value = inputNode.getCalculatedResult();

            // If any value is NOT Boolean.TRUE, the condition fails.
            if (!Boolean.TRUE.equals(value)) {
                return false;
            }
        }

        // If the loop completes, all values were Boolean.TRUE.
        return true;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}