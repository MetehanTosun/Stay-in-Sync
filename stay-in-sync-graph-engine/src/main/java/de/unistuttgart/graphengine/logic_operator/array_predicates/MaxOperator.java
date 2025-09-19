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

public class MaxOperator implements Operation {

    /**
     * Validates that the LogicNode is correctly configured for the MAX operation.
     * <p>
     * This method performs structural validation to ensure the node has at least one input
     * before the graph execution begins. It does not validate the actual content types
     * of the inputs, as those are checked during runtime execution.
     * <p>
     * Requirements:
     * <ul>
     *     <li>The node must have at least 1 input connection</li>
     *     <li>Each input should provide an array or collection of values</li>
     * </ul>
     *
     * @param node The LogicNode to validate for structural correctness
     * @throws OperatorValidationException if the node has no inputs or if the inputs list is null
     */
    @Override
    public void validateNode(LogicNode node) throws OperatorValidationException {
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.isEmpty()) {
            throw new OperatorValidationException(
                    "MAX operation for node '" + node.getName() + "' requires at least 1 array input."
            );
        }
    }

    /**
     * Executes the maximum-finding operation on all numeric values from all array/collection inputs.
     * <p>
     * This method processes each input node expecting arrays or collections, and compares
     * all numeric values to find the largest one. The comparison follows these rules:
     * <ul>
     *     <li>Only numeric values (instances of {@link Number}) are considered for comparison</li>
     *     <li>Non-numeric values are silently ignored</li>
     *     <li>Null inputs are skipped</li>
     *     <li>Non-array/collection inputs are ignored</li>
     *     <li>Empty arrays or collections contribute no values to the comparison</li>
     *     <li>All numeric comparisons are performed using double-precision arithmetic</li>
     * </ul>
     * <p>
     * If no numeric values are found across all inputs, returns 0.0 as a sensible default.
     *
     * @param node The LogicNode being evaluated, containing references to input nodes
     * @param dataContext The runtime data context (may be null, not used in this operation)
     * @return A {@link Double} representing the largest numeric value found across
     *         all array inputs. Returns 0.0 if no numeric values are found.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<Node> inputs = node.getInputNodes();
        Double maxValue = null;

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
                        double currentValue = ((Number) item).doubleValue();
                        if (maxValue == null || currentValue > maxValue) {
                            maxValue = currentValue;
                        }
                    }
                }
            }
            // Handle Arrays
            else if (result.getClass().isArray()) {
                int length = Array.getLength(result);
                for (int i = 0; i < length; i++) {
                    Object item = Array.get(result, i);
                    if (item instanceof Number) {
                        double currentValue = ((Number) item).doubleValue();
                        if (maxValue == null || currentValue > maxValue) {
                            maxValue = currentValue;
                        }
                    }
                }
            }
        }
        return maxValue != null ? maxValue : 0.0;
    }

    @Override
    public Class<?> getReturnType(){
        return Double.class;
    }
}