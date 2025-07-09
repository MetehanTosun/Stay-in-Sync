package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.object_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.Node;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class HasNoKeysOperator implements Operation {

    /**
     * Validates that the node has exactly two inputs.
     * The first input is expected to provide the JSON object and the second a Collection of keys.
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node does not have exactly two inputs.
     */
    @Override
    public void validate(LogicNode node) {
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.size() != 2) {
            throw new IllegalArgumentException(
                    "HAS_NO_KEYS operation for node '" + node.getName() + "' requires exactly 2 inputs: the JSON object and a Collection of key strings."
            );
        }
    }

    /**
     * Executes the key absence check.
     * It verifies that the provided JSON object contains none of the keys from the specified collection.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the first input value is null, not a JSON object, or if it is one
     * but does not have any field matching a name in the provided key collection.
     * Returns {@code false} if any of the specified keys are present.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<Node> inputs = node.getInputNodes();

        Object objectProvider = inputs.get(0).getCalculatedResult();
        Object keysProvider = inputs.get(1).getCalculatedResult();

        //if the object itself is null (e.g. path not found), it has no keys.
        if (objectProvider == null) {
            return true;
        }

        // If the provided value is not a JsonNode object, it also cannot have any keys.
        if (!(objectProvider instanceof JsonNode) || !((JsonNode) objectProvider).isObject()) {
            return true;
        }
        JsonNode jsonNode = (JsonNode) objectProvider;

        // If the list of keys to check is null or not a collection, we cannot find any keys, so the condition holds.
        if (keysProvider == null || !(keysProvider instanceof Collection)) {
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