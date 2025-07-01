package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.object_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;

import java.util.List;
import java.util.Map;

public class LacksKeyOperator implements Operation {

    /**
     * Validates that the node has exactly two inputs: the JSON object and the key name.
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node does not have exactly two inputs.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();
        if (inputs == null || inputs.size() != 2) {
            throw new IllegalArgumentException(
                    "LACKS_KEY operation for node '" + node.getNodeName() + "' requires exactly 2 inputs: the JSON object and the key (String)."
            );
        }
    }

    /**
     * Executes the key absence check.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the first input is not a JSON object or if it is a
     *         JSON object that does not have a field matching the name provided by the
     *         second input. Returns {@code false} if the key is present.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<InputNode> inputs = node.getInputProviders();
        Object objectProvider;
        Object keyProvider;

        try {
            objectProvider = inputs.get(0).getValue(dataContext);
            keyProvider = inputs.get(1).getValue(dataContext);
        } catch (IllegalStateException e) {
            // An input is missing. If the object is missing, it "lacks" the key.
            if (e.getMessage().contains("source '"+inputs.get(0).toString())) {
                return true;
            }
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
}