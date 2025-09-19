package de.unistuttgart.graphengine.logic_operator.array_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.Operation;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SumOperator implements Operation {

    /**
     * Validates that the node is correctly configured for the SUM operation.
     * <p>
     * This check ensures that the operator has at least one input node and that
     * all inputs are arrays or collections containing numeric values.
     * @param node The LogicNode to validate.
     * @throws OperatorValidationException if the node has no inputs.
     */
    @Override
    public void validateNode(LogicNode node) throws OperatorValidationException {
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.isEmpty()) {
            throw new OperatorValidationException(
                    "SUM operation for node '" + node.getName() + "' requires at least 1 array input."
            );
        }
    }

    /**
     * Executes the summation on all numeric values from all array/collection inputs.
     * <p>
     * This method processes each input node expecting arrays or collections, and extracts
     * all numeric values for summation. Non-array/collection inputs are ignored.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return A {@link Double} representing the sum of all numeric values found across
     *         all array inputs. Returns 0.0 if no numeric values are found or if no
     *         valid array inputs are provided.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<Node> inputs = node.getInputNodes();
        double sum = 0.0;

        // Process each input node
        for (Node inputNode : inputs) {
            Object result = inputNode.getCalculatedResult();

            if (result == null) {
                continue; // Skip null results
            }

            // Handle Collections (Lists, Sets, etc.)
            if (result instanceof Collection) {
                Collection<?> collection = (Collection<?>) result;
                for (Object item : collection) {
                    if (item instanceof Number) {
                        sum += ((Number) item).doubleValue();
                    }
                    // Non-numeric items are silently ignored
                }
            }
            // Handle Arrays
            else if (result.getClass().isArray()) {
                int length = Array.getLength(result);
                for (int i = 0; i < length; i++) {
                    Object item = Array.get(result, i);
                    if (item instanceof Number) {
                        sum += ((Number) item).doubleValue();
                    }
                }
            }
        }

        return sum;
    }

    @Override
    public Class<?> getReturnType(){
        return Double.class;
    }
}