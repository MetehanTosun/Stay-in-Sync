package de.unistuttgart.graphengine.logic_operator.array_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.Operation;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AvgOperator implements Operation {


    /**
     * Validates that the LogicNode is correctly configured for the AVG operation.
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
                    "AVG operation for node '" + node.getName() + "' requires at least 1 array input."
            );
        }
    }

    /**
     * Executes the average calculation on all numeric values from all array/collection inputs.
     * <p>
     * This method processes each input node expecting arrays or collections, and extracts
     * all numeric values for average calculation. The calculation follows these rules:
     * <ul>
     *     <li>Only numeric values (instances of {@link Number}) are included in the calculation</li>
     *     <li>Non-numeric values are silently ignored</li>
     *     <li>Null inputs are skipped</li>
     *     <li>Non-array/collection inputs are ignored</li>
     *     <li>Empty arrays or collections contribute no values to the calculation</li>
     * </ul>
     * <p>
     * The average is computed as: sum of all numeric values / count of numeric values.
     * If no numeric values are found, returns 0.0 to prevent division by zero.
     *
     * @param node The LogicNode being evaluated, containing references to input nodes
     * @param dataContext The runtime data context (may be null, not used in this operation)
     * @return A {@link Double} representing the arithmetic average of all numeric values
     *         found across all array inputs. Returns 0.0 if no numeric values are found.
     */
    @Override
    public Object execute(LogicNode node, Map<String, Object> dataContext) {
        List<Node> inputs = node.getInputNodes();
        double sum = 0.0;
        int count = 0;

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
                        count++;
                    }
                }
            }
            // Handle Arrays
            else if (result.getClass().isArray()) {
                int length = Array.getLength(result);
                for (int i = 0; i < length; i++) {
                    Object item = Array.get(result, i);
                    if (item instanceof Number) {
                        sum += ((Number) item).doubleValue();
                        count++;
                    }
                }
            }
        }
        return (count == 0) ? 0.0 : sum / count;
    }

    @Override
    public Class<?> getReturnType(){
        return Double.class;
    }
}