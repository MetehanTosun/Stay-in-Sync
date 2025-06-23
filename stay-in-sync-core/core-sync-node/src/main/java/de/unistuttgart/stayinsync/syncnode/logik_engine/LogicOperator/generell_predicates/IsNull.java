package de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.generell_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.JsonInputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.JsonPathValueExtractor;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.Operation;

import java.util.List;
import java.util.Map;

public class IsNull implements Operation {

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


            boolean isNull = false;

            if (dataContext != null) {
                JsonNode sourceObject = dataContext.get(sourceName);
                if (sourceObject != null) {
                    if (valueExtractor.pathExists(sourceObject, jsonPath)) {
                        if (valueExtractor.extractValue(sourceObject, jsonPath).isEmpty()) {
                            isNull = true;
                        }
                    }
                }
            }
            if (!isNull) {
                return false;
            }
        }
        return true;
    }
}
