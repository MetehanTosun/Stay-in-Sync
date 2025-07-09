package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.array_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.Node;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AvgOperator implements Operation {

    /**
     * Validates that the node is correctly configured for the AVG (average) operation.
     * <p>
     * This structural check ensures that the operator is provided with exactly one input,
     * which should be the array or collection for which the average is to be calculated.
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node does not have exactly one input.
     */
    @Override
    public void validate(LogicNode node) {
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.size() != 1) {
            throw new IllegalArgumentException(
                    "AVG operation for node '" + node.getName() + "' requires exactly 1 input: the collection of numbers."
            );
        }
    }

    /**
     * Executes the average calculation on the pre-calculated result of its input node.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return A {@link Double} representing the average. Returns 0.0 if the input is missing,
     * null, not a collection/array, or contains no numeric elements.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        // Get the single input node.
        Node inputNode = node.getInputNodes().get(0);

        // Get the pre-calculated result, which should be a collection or an array.
        Object collectionProvider = inputNode.getCalculatedResult();

        // If the upstream node failed to produce a result, or it is not a collection/array, the average is 0.0.
        if (collectionProvider == null || (!(collectionProvider instanceof Collection) && !collectionProvider.getClass().isArray())) {
            return 0.0;
        }

        double sum = 0.0;
        int count = 0;

        if (collectionProvider instanceof Collection) {
            for (Object item : (Collection<?>) collectionProvider) {
                if (item instanceof Number) {
                    sum += ((Number) item).doubleValue();
                    count++;
                }
            }
        } else {
            int length = Array.getLength(collectionProvider);
            for (int i = 0; i < length; i++) {
                Object item = Array.get(collectionProvider, i);
                if (item instanceof Number) {
                    sum += ((Number) item).doubleValue();
                    count++;
                }
            }
        }

        // Avoid division by zero.
        return (count == 0) ? 0.0 : sum / count;
    }
}