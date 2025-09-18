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

public class IsNotNullOperator implements Operation {

    private final JsonPathValueExtractor valueExtractor = new JsonPathValueExtractor();

    /**
     * Validates that the node has at least one input, and all inputs are ProviderNodes.
     *
     * @param node The LogicNode to validate.
     * @throws OperatorValidationException if the configuration is invalid.
     */
    @Override
    public void validateNode(LogicNode node)throws OperatorValidationException {
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
            Object value = inputNode.getCalculatedResult();
            if (value == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}