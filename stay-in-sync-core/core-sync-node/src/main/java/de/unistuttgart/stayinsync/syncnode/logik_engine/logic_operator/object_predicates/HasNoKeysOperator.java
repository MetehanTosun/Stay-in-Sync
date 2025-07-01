package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.object_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class HasNoKeysOperator implements Operation {

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
                    "HAS_NO_KEYS operation for node '" + node.getNodeName() + "' requires exactly 2 inputs: the JSON object and a Collection of key strings."
            );
        }
    }

    /**
     * Executes the key absence check. It verifies that the provided JSON object
     * contains none of the keys from the specified collection.
     *
     * @param node        The LogicNode being evaluated. Its first input must provide a
     * JsonNode object, and the second a Collection of Strings (the keys).
     * @param dataContext The runtime data context.
     * @return {@code true} if the first input is not a JSON object or if it is one
     * but does not have any field matching a name in the provided key collection.
     * Returns {@code false} if any of the specified keys are present.
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
            // If the object-input is missing, it "has no keys", which is true for this operator.
            return true;
        }

        // If the provider is not a JsonNode object, it cannot have any keys.
        if (!(objectProvider instanceof JsonNode) || !((JsonNode) objectProvider).isObject()) {
            return true;
        }
        JsonNode jsonNode = (JsonNode) objectProvider;

        // The second input must be a collection.
        if (!(keysProvider instanceof Collection)) {
            // If the key list is invalid, we cannot prove a key exists.
            return true;
        }
        Collection<?> keys = (Collection<?>) keysProvider;

        // Check that no key from the collection exists in the JsonNode.
        for (Object keyObj : keys) {
            if (keyObj instanceof String) {
                String key = (String) keyObj;
                if (jsonNode.has(key)) {
                    return false; // Found a key that should not be there.
                }
            }
        }

        // If the loop completes, none of the specified keys were found.
        return true;
    }
}