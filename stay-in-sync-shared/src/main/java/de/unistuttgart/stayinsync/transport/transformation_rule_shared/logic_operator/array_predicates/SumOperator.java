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

public class SumOperator implements Operation {

    /**
     * Validates that the node is correctly configured for the SUM operation.
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
                    "SUM operation for node '" + node.getName() + "' requires exactly 1 input: the collection of numbers to sum up."
            );
        }
    }

    /**
     * Executes the summation on the pre-calculated result of its input node.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return A {@link Double} representing the sum of all numeric elements. Returns 0.0 if the
     * input is null, not a collection/array, or contains no numeric elements.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        Node inputNode = node.getInputNodes().get(0);

        Object collectionProvider = inputNode.getCalculatedResult();

        // If the upstream node failed to produce a result, or it's not a valid type, the sum is 0.0.
        if (collectionProvider == null || (!(collectionProvider instanceof Collection) && !collectionProvider.getClass().isArray())) {
            return 0.0;
        }

        double sum = 0.0;

        // Logic for Collections
        if (collectionProvider instanceof Collection) {
            for (Object item : (Collection<?>) collectionProvider) {
                if (item instanceof Number) {
                    sum += ((Number) item).doubleValue();
                }
            }
        }
        // Logic for Arrays
        else {
            int length = Array.getLength(collectionProvider);
            for (int i = 0; i < length; i++) {
                Object item = Array.get(collectionProvider, i);
                if (item instanceof Number) {
                    sum += ((Number) item).doubleValue();
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