package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.object_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class HasAnyKeyOperator implements Operation {

    /**
     * Validates that the node has exactly two inputs.
     * The first input is expected to provide the JSON object and the second a Collection of keys.
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node does not have exactly two inputs.
     */
    @Override
    public void validateNode(LogicNode node) {
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.size() != 2) {
            throw new IllegalArgumentException(
                    "HAS_ANY_KEY operation for node '" + node.getName() + "' requires exactly 2 inputs: the JSON object and a Collection of key strings."
            );
        }
    }

    /**
     * Executes the key existence check.
     * It verifies if the provided JSON object contains at least one of the keys from the specified collection.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the first input is a JSON object and has at least one field
     * matching a name in the provided key collection. Returns {@code false} otherwise.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<Node> inputs = node.getInputNodes();

        Object objectProvider = inputs.get(0).getCalculatedResult();
        Object keysProvider = inputs.get(1).getCalculatedResult();

        // If either input could not be resolved, the condition cannot be met.
        if (objectProvider == null || keysProvider == null) {
            return false;
        }

        // The first input must be a JsonNode representing an object.
        if (!(objectProvider instanceof JsonNode) || !((JsonNode) objectProvider).isObject()) {
            return false;
        }
        JsonNode jsonNode = (JsonNode) objectProvider;

        // The second input must be a collection.
        if (!(keysProvider instanceof Collection)) {
            return false;
        }
        Collection<?> keys = (Collection<?>) keysProvider;

        // Check if any key from the collection exists in the JsonNode.
        for (Object keyObj : keys) {
            if (keyObj instanceof String) {
                String key = (String) keyObj;
                if (jsonNode.has(key)) {
                    return true; // Found at least one matching key.
                }
            }
        }

        // If the loop completes, no keys were found.
        return false;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}