package de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.array_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.Operation;

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
     * @param node The LogicNode to validate. It must contain a list of input providers.
     * @throws IllegalArgumentException if the node does not have exactly one input.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();
        if (inputs == null || inputs.size() != 1) {
            throw new IllegalArgumentException(
                    "AVG operation for node '" + node.getNodeName() + "' requires exactly 1 input: the collection of numbers."
            );
        }
    }

    /**
     * Executes the average calculation.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return A {@link Double} representing the average. Returns 0.0 if the input is missing,
     *         not a collection, or contains no numeric elements.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<InputNode> inputs = node.getInputProviders();
        Object collectionProvider;

        try {
            collectionProvider = inputs.get(0).getValue(dataContext);
        } catch (IllegalStateException e) {
            return 0.0;
        }

        if (!(collectionProvider instanceof Collection) && !collectionProvider.getClass().isArray()) {
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