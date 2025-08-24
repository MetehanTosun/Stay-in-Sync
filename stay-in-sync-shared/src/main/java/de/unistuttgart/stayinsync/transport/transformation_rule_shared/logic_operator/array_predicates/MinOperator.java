package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.array_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.exception.OperatorValidationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MinOperator implements Operation {

    /**
     * Validates that the node is correctly configured for the MIN operation.
     * <p>
     * This check ensures that the operator has exactly one input node, which is
     * expected to deliver the array or collection of numbers.
     *
     * @param node The LogicNode to validate.
     * @throws OperatorValidationException if the node does not have exactly one input.
     */
    @Override
    public void validateNode(LogicNode node)throws OperatorValidationException {
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.size() != 1) {
            throw new OperatorValidationException(
                    "MIN operation for node '" + node.getName() + "' requires exactly 1 input."
            );
        }
    }

    /**
     * Executes the minimum-finding logic on the pre-calculated result of its input node.
     * <p>
     * It iterates through all elements of the input, considering only those that are
     * instances of {@link Number}. The internal comparison is done using double values.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return A {@link Double} representing the minimum value found. If the input is
     * null, not a collection/array, or contains no numeric elements,
     * it returns {@code 0.0} as a default value.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        Node inputNode = node.getInputNodes().get(0);

        Object collectionProvider = inputNode.getCalculatedResult();

        if (collectionProvider == null || (!(collectionProvider instanceof Collection) && !collectionProvider.getClass().isArray())) {
            return 0.0;
        }

        Double min = null;

        // Handle Collections
        if (collectionProvider instanceof Collection) {
            for (Object item : (Collection<?>) collectionProvider) {
                if (item instanceof Number) {
                    double currentValue = ((Number) item).doubleValue();
                    if (min == null || currentValue < min) {
                        min = currentValue;
                    }
                }
            }
        }
        // Handle native Java arrays
        else {
            int length = Array.getLength(collectionProvider);
            for (int i = 0; i < length; i++) {
                Object item = Array.get(collectionProvider, i);
                if (item instanceof Number) {
                    double currentValue = ((Number) item).doubleValue();
                    if (min == null || currentValue < min) {
                        min = currentValue;
                    }
                }
            }
        }

        // If no numbers were found, min is still null. Return 0.0 in that case.
        return (min == null) ? 0.0 : min;
    }

    @Override
    public Class<?> getReturnType(){
        return Double.class;
    }
}