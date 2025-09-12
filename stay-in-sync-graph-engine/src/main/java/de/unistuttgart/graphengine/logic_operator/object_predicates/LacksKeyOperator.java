package de.unistuttgart.graphengine.logic_operator.object_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.Operation;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;

import java.util.List;
import java.util.Map;

public class LacksKeyOperator implements Operation {

    /**
     * Validates that the node has exactly two inputs: the JSON object and the key name.
     *
     * @param node The LogicNode to validate.
     * @throws OperatorValidationException if the node does not have exactly two inputs.
     */
    @Override
    public void validateNode(LogicNode node)throws OperatorValidationException {
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.size() != 2) {
            throw new OperatorValidationException(
                    "LACKS_KEY operation for node '" + node.getName() + "' requires exactly 2 inputs: the JSON object and the key (String)."
            );
        }
    }

    /**
     * Executes the key absence check.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the first input value is null, not a JSON object, or if it is one
     * but does not have a field matching the name provided by the second input.
     * Returns {@code false} if the key is present.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<Node> inputs = node.getInputNodes();

        Object objectProvider = inputs.get(0).getCalculatedResult();
        Object keyProvider = inputs.get(1).getCalculatedResult();

        // If the key itself is missing, we cannot perform the check, so we fail-safe to false.
        if (keyProvider == null) {
            return false;
        }

        // The second input must be a String representing the key name.
        if (!(keyProvider instanceof String)) {
            return false;
        }
        String key = (String) keyProvider;

        // If the first input is not a JsonNode or not an OBJECT node, it cannot have the key.
        if (!(objectProvider instanceof JsonNode) || !((JsonNode) objectProvider).isObject()) {
            return true;
        }
        JsonNode jsonNode = (JsonNode) objectProvider;

        // Simply negate the result of 'has'. If it has the key, 'lacks' is false.
        return !jsonNode.has(key);
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}