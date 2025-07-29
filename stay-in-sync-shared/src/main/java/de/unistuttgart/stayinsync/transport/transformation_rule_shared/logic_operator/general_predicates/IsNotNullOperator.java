package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.exception.OperatorValidationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.ProviderNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.JsonPathValueExtractor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class IsNotNullOperator implements Operation {

    private final JsonPathValueExtractor valueExtractor = new JsonPathValueExtractor();

    /**
     * Validates that the node has at least one input, and all inputs are ProviderNodes.
     *
     * @param node The LogicNode to validate.
     * @throws OperatorValidationException if the configuration is invalid.
     */
    @Override
    public void validateNode(LogicNode node) {
        List<Node> inputs = node.getInputNodes();

        if (inputs == null || inputs.isEmpty()) {
            throw new OperatorValidationException(
                    "IS_NOT_NULL operation for node '" + node.getName() + "' requires at least 1 input."
            );
        }

        for (Node input : inputs) {
            if (!(input instanceof ProviderNode)) {
                throw new OperatorValidationException(
                        "IS_NOT_NULL operation for node '" + node.getName() + "' requires all its inputs to be of type ProviderNode, but found " + input.getClass().getSimpleName()
                );
            }
        }
    }

    /**
     * Executes the is-not-null check for one or more paths.
     * <p>
     * For each input, it checks that the path exists AND that the value at that path
     * is not explicitly null. It returns true only if this condition holds for all inputs.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if all paths exist and their values are not null, otherwise {@code false}.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        for (Node inputNode : node.getInputNodes()) {
            ProviderNode provider = (ProviderNode) inputNode;

            // 1. Parse the path from the ProviderNode
            String fullPath = provider.getJsonPath();
            if (fullPath == null || !fullPath.startsWith("source.")) {
                return false;
            }
            String[] parts = fullPath.split("\\.");
            if (parts.length < 2) {
                return false;
            }
            String sourceName = parts[1];
            String internalJsonPath = (parts.length > 2) ? String.join(".", Arrays.copyOfRange(parts, 2, parts.length)) : "";

            // 2. Get the source object from the context
            JsonNode sourceObject = dataContext.get(sourceName);

            // 3. Perform the check: extractValue().isPresent() is true only if the path
            // exists AND the value is not null. This is exactly what we need.
            boolean isPresentAndNotNull = valueExtractor.extractValue(sourceObject, internalJsonPath).isPresent();

            if (!isPresentAndNotNull) {
                // If a single path doesn't exist or its value is null, the AND-conjunction fails.
                return false;
            }
        }

        // If the loop completes, all paths existed and had non-null values.
        return true;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}