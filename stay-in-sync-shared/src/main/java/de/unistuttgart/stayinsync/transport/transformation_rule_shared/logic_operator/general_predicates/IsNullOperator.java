package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.syncnode.logic_engine.nodes.ProviderNode;

import java.util.List;
import java.util.Map;

public class IsNullOperator implements Operation {

    /**
     * Validates that the node has at least one input, and all inputs are ProviderNodes.
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the configuration is invalid.
     */
    @Override
    public void validateNode(LogicNode node) {
        List<Node> inputs = node.getInputNodes();

        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException(
                    "IS_NULL operation for node '" + node.getName() + "' requires at least 1 input."
            );
        }

        for (Node input : inputs) {
            if (!(input instanceof ProviderNode)) {
                throw new IllegalArgumentException(
                        "IS_NULL operation for node '" + node.getName() + "' requires all its inputs to be of type ProviderNode."
                );
            }
        }
    }

    /**
     * Checks if the value for each provided path exists and is explicitly null.
     * <p>
     * This operator returns true if and only if for all inputs, the path resolves
     * successfully to a {@code null} value. It returns false if any path does not
     * exist or if the value is not null.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if all paths exist and their values are explicitly null.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        for (Node inputNode : node.getInputNodes()) {
            Object value = inputNode.getCalculatedResult();

            // If the path did not exist, `value` would be the special `ProviderNode.PATH_NOT_FOUND` object,
            // so `value == null` would be false.
            // The check is only true if the path existed and its value was actually null.
            if (value != null) {
                // If the value is not null (this includes the PATH_NOT_FOUND case), the condition fails.
                return false;
            }
        }
        // If the loop completes, all paths existed and their values were null.
        return true;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}