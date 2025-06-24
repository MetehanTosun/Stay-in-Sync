package de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.array_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.Operation;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MinOperator implements Operation {

    /**
     * Validates that the node is correctly configured for the MIN operation.
     * <p>
     * This check ensures that the operator has exactly one input provider, which is
     * expected to deliver the array or collection of numbers.
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node does not have exactly one input.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();
        if (inputs == null || inputs.size() != 1) {
            throw new IllegalArgumentException(
                    "MIN operation for node '" + node.getNodeName() + "' requires exactly 1 input."
            );
        }
    }

    /**
     * Executes the minimum-finding logic.
     * <p>
     * It iterates through all elements of the input, considering only those that are
     * instances of {@link Number}. The internal comparison is done using double values
     * to handle different numeric types consistently.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return A {@link Double} representing the minimum value found. If the input is
     *         missing, not a collection/array, or contains no numeric elements,
     *         it returns {@code 0.0} as a sensible default.
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
}