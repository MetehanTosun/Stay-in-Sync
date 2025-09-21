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
    public Object execute(LogicNode node, Map<String, Object> dataContext) {
        if (dataContext == null) {
            return true;
        }

        for (Node input : node.getInputNodes()) {
            if (input instanceof ProviderNode) {
                ProviderNode pNode = (ProviderNode) input;
                String fullPath = pNode.getJsonPath();

                String[] parts = fullPath.split("\\.", 2);
                if (parts.length == 0) continue;

                String sourceKey = parts[0];
                Object sourceDataAsObject = dataContext.get(sourceKey);

                // If the source key itself doesn't exist, the path doesn't exist. Continue.
                if (sourceDataAsObject == null) {
                    continue;
                }

                // If the data for the source key is not a JsonNode, the path can't exist within it. Continue.
                if (!(sourceDataAsObject instanceof JsonNode)) {
                    continue;
                }
                JsonNode sourceObject = (JsonNode) sourceDataAsObject;

                String internalJsonPath = (parts.length > 1) ? parts[1] : "";

                if (valueExtractor.pathExists(sourceObject, internalJsonPath)) {
                    return false;
                }
            }
        }
        // If the loop completes, no path was found to exist.
        return true;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}
