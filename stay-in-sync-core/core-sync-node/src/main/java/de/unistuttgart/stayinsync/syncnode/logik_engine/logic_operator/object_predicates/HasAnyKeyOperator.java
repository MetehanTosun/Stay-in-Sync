package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.object_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class HasAnyKeyOperator implements Operation {

    /**
     * Validates that the node has exactly two inputs.
     * The first input is expected to be the JSON object and the second a Collection of keys.
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node does not have exactly two inputs.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();
        if (inputs == null || inputs.size() != 2) {
            throw new IllegalArgumentException(
                    "HAS_ANY_KEY operation for node '" + node.getNodeName() + "' requires exactly 2 inputs: the JSON object and a Collection of key strings."
            );
        }
    }

    /**
     * Executes the key existence check. It verifies if the provided JSON object
     * contains at least one of the keys from the specified collection.
     *
     * @param node        The LogicNode being evaluated. Its first input must provide a
     * JsonNode object, and the second a Collection of Strings (the keys).
     * @param dataContext The runtime data context.
     * @return {@code true} if the first input is a JSON object and has at least one field
     * matching a name in the provided key collection. Returns {@code false} otherwise.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<InputNode> inputs = node.getInputProviders();
        Object objectProvider;
        Object keysProvider;

        try {
            objectProvider = inputs.get(0).getValue(dataContext);
            keysProvider = inputs.get(1).getValue(dataContext);
        } catch (IllegalStateException e) {
            return false; // An input is missing.
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
}