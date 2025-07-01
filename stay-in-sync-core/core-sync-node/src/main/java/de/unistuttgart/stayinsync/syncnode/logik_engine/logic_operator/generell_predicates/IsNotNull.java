package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.generell_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.JsonInputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.JsonPathValueExtractor;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;

import java.util.List;
import java.util.Map;

public class IsNotNull implements Operation {
    private final JsonPathValueExtractor valueExtractor = new JsonPathValueExtractor();

    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();

        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException(
                    "IS_NULL operation for node '" + node.getNodeName() + "' requires at least 1 input."
            );
        }

        for (InputNode input : inputs) {
            if (!(input instanceof JsonInputNode)) {
                throw new IllegalArgumentException(
                        "IS_NULL operation for node '" + node.getNodeName() + "' requires all its inputs to be of type JsonInputNode, but found " + input.getClass().getSimpleName()
                );
            }
        }
    }

    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        for (InputNode inputProvider : node.getInputProviders()) {
            JsonInputNode jsonInput = (JsonInputNode) inputProvider;
            String sourceName = jsonInput.getSourceName();
            String jsonPath = jsonInput.getJsonPath();

            boolean isNotNull = false;

            if (dataContext != null) {
                JsonNode sourceObject = dataContext.get(sourceName);
                if (sourceObject != null) {
                    if (valueExtractor.pathExists(sourceObject, jsonPath)) {
                        if (valueExtractor.extractValue(sourceObject, jsonPath).isPresent()) {
                            isNotNull = true;
                        }
                    }
                }
            }

            if (!isNotNull) {
                return false;
            }
        }
        return true;
    }

}
