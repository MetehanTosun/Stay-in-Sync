package de.unistuttgart.graphengine.logic_operator.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.Operation;
import de.unistuttgart.graphengine.nodes.JsonPathValueExtractor;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;
import de.unistuttgart.graphengine.nodes.ProviderNode;

import java.util.List;
import java.util.Map;

public class ExistsOperator implements Operation {

    private final JsonPathValueExtractor valueExtractor = new JsonPathValueExtractor();

    @Override
    public void validateNode(LogicNode node)throws OperatorValidationException {
        // Diese Validierung bleibt gleich und stellt sicher, dass alle Inputs ProviderNodes sind.
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.isEmpty()) {
            throw new OperatorValidationException("EXISTS operation requires at least 1 input.");
        }
        for (Node input : inputs) {
            if (!(input instanceof ProviderNode)) {
                throw new OperatorValidationException("EXISTS operation requires all inputs to be of type ProviderNode.");
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
    public Object execute(LogicNode node, Map<String, Object> dataContext) {
        if (dataContext == null || dataContext.isEmpty()) {
            return false;
        }

        for (Node inputNode : node.getInputNodes()) {
            ProviderNode provider = (ProviderNode) inputNode;
            String fullPath = provider.getJsonPath();

            String[] parts = fullPath.split("\\.", 2);
            if (parts.length == 0) {
                return false;
            }

            String sourceKey = parts[0];

            Object sourceDataAsObject = dataContext.get(sourceKey);
            if (sourceDataAsObject == null) {
                return false;
            }

            // Type check before casting
            if (!(sourceDataAsObject instanceof JsonNode)) {
                return false;
            }
            JsonNode sourceObject = (JsonNode) sourceDataAsObject;
            String internalJsonPath = (parts.length > 1) ? parts[1] : "";

            if (!valueExtractor.pathExists(sourceObject, internalJsonPath)) {
                return false;
            }
        }
        return true;
    }
    @Override
    public Class<?> getReturnType() {
        return Boolean.class;
    }
}