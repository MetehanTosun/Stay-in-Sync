package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.generell_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.JsonPathValueExtractor;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.Node;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.ProviderNode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExistsOperator implements Operation {

    private final JsonPathValueExtractor valueExtractor = new JsonPathValueExtractor();

    @Override
    public void validate(LogicNode node) {
        // Diese Validierung bleibt gleich und stellt sicher, dass alle Inputs ProviderNodes sind.
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("EXISTS operation requires at least 1 input.");
        }
        for (Node input : inputs) {
            if (!(input instanceof ProviderNode)) {
                throw new IllegalArgumentException("EXISTS operation requires all inputs to be of type ProviderNode.");
            }
        }
    }

    /**
     * Executes the existence check by directly validating the path for each ProviderNode input.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if all paths defined in the input ProviderNodes exist, otherwise {@code false}.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {

        for (Node inputNode : node.getInputNodes()) {
            ProviderNode provider = (ProviderNode) inputNode;

            String fullPath = provider.getJsonPath();
            if (fullPath == null || !fullPath.startsWith("source.")) { return false; }

            String[] parts = fullPath.split("\\.");
            if (parts.length < 2) { return false; }

            String sourceName = parts[1];
            String internalJsonPath = (parts.length > 2) ? String.join(".", Arrays.copyOfRange(parts, 2, parts.length)) : "";

            JsonNode sourceObject = dataContext.get(sourceName);

            if (!valueExtractor.pathExists(sourceObject, internalJsonPath)) {
                return false;
            }
        }

        return true;
    }
}