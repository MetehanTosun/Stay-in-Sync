package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.object_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.exception.OperatorValidationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class HasAllKeysOperator implements Operation {

    /**
     * Validates that the node has exactly two inputs.
     * The first input is expected to provide the JSON object and the second a Collection of keys.
     *
     * @param node The LogicNode to validate.
     * @throws OperatorValidationException if the node does not have exactly two inputs.
     */
    @Override
    public void validateNode(LogicNode node)throws OperatorValidationException {
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.size() != 2) {
            throw new OperatorValidationException(
                    "HAS_ALL_KEYS operation for node '" + node.getName() + "' requires exactly 2 inputs: the JSON object and a Collection of key strings."
            );
        }
    }

    /**
     * Executes the key existence check.
     * It verifies if the provided JSON object contains all keys from the specified collection.
     *
     * @param node        The LogicNode being evaluated. Its first input must provide a
     * JsonNode object, and the second a Collection of Strings (the keys).
     * @param dataContext The runtime data context.
     * @return {@code true} if the first input is a JSON object and has every field
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

        // The second input must be a collection (e.g., List or Set).
        if (!(keysProvider instanceof Collection)) {
            return false;
        }
        Collection<?> keys = (Collection<?>) keysProvider;
        if (keys.isEmpty()) {
            return true; // Vacuously true: an empty set of required keys is always present.
        }

        // Check if all keys from the collection exist in the JsonNode.
        for (Object keyObj : keys) {
            if (!(keyObj instanceof String)) {
                return false; // All elements in the key collection must be strings.
            }
            String key = (String) keyObj;
            if (!jsonNode.has(key)) {
                return false; // Found a required key that is missing.
            }
        }

        // If the loop completes, all keys were found.
        return true;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}