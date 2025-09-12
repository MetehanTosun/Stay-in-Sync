package de.unistuttgart.graphengine.logic_operator.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.Operation;
import de.unistuttgart.graphengine.nodes.JsonPathValueExtractor;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;
import de.unistuttgart.graphengine.nodes.ProviderNode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class NotExistsOperator implements Operation {

    private final JsonPathValueExtractor valueExtractor = new JsonPathValueExtractor();

    /**
     * Validates that the LogicNode is correctly configured for the NOT_EXISTS operation.
     * <p>
     * This operation requires the node to have one or more input providers, all of which
     * must be instances of {@link ProviderNode}.
     *
     * @param node The LogicNode to validate.
     * @throws OperatorValidationException if the node's configuration is invalid.
     */
    @Override
    public void validateNode(LogicNode node)throws OperatorValidationException {
        List<Node> inputs = node.getInputNodes();

        if (inputs == null || inputs.isEmpty()) {
            throw new OperatorValidationException(
                    "NOT_EXISTS operation for node '" + node.getName() + "' requires at least 1 input."
            );
        }

        for (Node input : inputs) {
            if (!(input instanceof ProviderNode)) {
                throw new OperatorValidationException(
                        "NOT_EXISTS operation for node '" + node.getName() + "' requires all its inputs to be of type ProviderNode, but found " + input.getClass().getSimpleName()
                );
            }
        }
    }

    /**
     * Executes the non-existence check for the NOT_EXISTS operation on one or more paths.
     * <p>
     * It iterates through all {@link ProviderNode} inputs. The final result is {@code true}
     * if and only if **none** of the specified paths exist in their respective data sources.
     *
     * @param node        The LogicNode currently being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if none of the paths exist, otherwise {@code false}.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        for (Node inputNode : node.getInputNodes()) {
            ProviderNode provider = (ProviderNode) inputNode;

            // 1. Parse the path from the ProviderNode
            String fullPath = provider.getJsonPath();
            if (fullPath == null || !fullPath.startsWith("source.")) {
                // An invalid path cannot exist, so the condition is met for this input.
                continue;
            }
            String[] parts = fullPath.split("\\.");
            if (parts.length < 2) {
                continue;
            }
            String sourceName = parts[1];
            String internalJsonPath = (parts.length > 2) ? String.join(".", Arrays.copyOfRange(parts, 2, parts.length)) : "";

            // 2. Get the source object from the context
            JsonNode sourceObject = dataContext.get(sourceName);

            // 3. Use the pathExists method. If any path exists, the "not exists" condition fails.
            if (valueExtractor.pathExists(sourceObject, internalJsonPath)) {
                return false;
            }
        }
        // If the loop completes, none of the paths existed.
        return true;
    }
    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}
