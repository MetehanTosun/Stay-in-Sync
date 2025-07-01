package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.generell_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.JsonInputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.JsonPathValueExtractor;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;

import java.util.List;
import java.util.Map;

public class NotExistsOperator implements Operation {

    private final JsonPathValueExtractor valueExtractor = new JsonPathValueExtractor();

    /**
     * Validates that the LogicNode is correctly configured for the NOT_EXISTS operation.
     * <p>
     * This operation requires the node to have one or more input providers, all of which
     * must be instances of {@link JsonInputNode}.
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node's configuration is invalid.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();

        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException(
                    "NOT_EXISTS operation for node '" + node.getNodeName() + "' requires at least 1 input."
            );
        }

        for (InputNode input : inputs) {
            if (!(input instanceof JsonInputNode)) {
                throw new IllegalArgumentException(
                        "NOT_EXISTS operation for node '" + node.getNodeName() + "' requires all its inputs to be of type JsonInputNode, but found " + input.getClass().getSimpleName()
                );
            }
        }
    }

    /**
     * Executes the non-existence check for the NOT_EXISTS operation on one or more paths.
     * <p>
     * It iterates through all {@link JsonInputNode} inputs. The final result is {@code true}
     * if and only if **none** of the specified paths exist in their respective data sources.
     *
     * @param node        The LogicNode currently being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if none of the paths exist, otherwise {@code false}.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        for (InputNode inputProvider : node.getInputProviders()) {
            JsonInputNode jsonInput = (JsonInputNode) inputProvider;
            String sourceName = jsonInput.getSourceName();
            String jsonPath = jsonInput.getJsonPath();

            boolean currentPathExists = false;
            if (dataContext != null) {
                JsonNode sourceObject = dataContext.get(sourceName);
                if (sourceObject != null) {
                    currentPathExists = valueExtractor.pathExists(sourceObject, jsonPath);
                }
            }
            if (currentPathExists) {
                return false;
            }
        }

        return true;
    }
}
