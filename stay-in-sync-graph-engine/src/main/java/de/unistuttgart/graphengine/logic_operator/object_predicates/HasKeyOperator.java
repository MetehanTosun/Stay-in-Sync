package de.unistuttgart.graphengine.logic_operator.object_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.Operation;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;

import java.util.List;
import java.util.Map;

public class HasKeyOperator implements Operation {

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
                    "HAS_KEY operation for node '" + node.getName() + "' requires exactly 2 inputs: the JSON object and the key (String)."
            );
        }
    }

    /**
     * Executes the key existence check.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the first input is a JSON object and has a field matching
     * the name provided by the second input. Returns {@code false} otherwise.
     */
    @Override
    public Object execute(LogicNode node, Map<String, Object> dataContext) {
        List<Node> inputs = node.getInputNodes();

        Object objectProvider = inputs.get(0).getCalculatedResult();
        Object keyProvider = inputs.get(1).getCalculatedResult();

        // If either input value is null, the check cannot succeed.
        if (objectProvider == null || keyProvider == null) {
            return false;
        }

        // The first input must be a JsonNode representing an object.
        if (!(objectProvider instanceof JsonNode)) {
            return false;
        }
        JsonNode jsonNode = (JsonNode) objectProvider;

        // The node must specifically be an OBJECT node (not an array, string, etc.).
        if (!jsonNode.isObject()) {
            return false;
        }

        // The second input must be a String representing the key name.
        if (!(keyProvider instanceof String)) {
            return false;
        }
        String key = (String) keyProvider;

        // Use Jackson's built-in method to check for the field's existence.
        // .has(key) returns true even if the value is 'null', which is the correct behavior.
        return jsonNode.has(key);
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}