package de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.array_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.Operation;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class NotEmptyOperator implements Operation {

    /**
     * Validates that the node has exactly one input, which is the array or collection to check.
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node does not have exactly one input.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();
        if (inputs == null || inputs.size() != 1) {
            throw new IllegalArgumentException(
                    "NOT_EMPTY operation for node '" + node.getNodeName() + "' requires exactly 1 input: the array or collection to check."
            );
        }
    }

    /**
     * Executes the not-empty check.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the input is an array or collection with at least one element.
     *         Returns {@code false} if the input is missing, not an array/collection, or is empty.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<InputNode> inputs = node.getInputProviders();
        Object arrayOrCollectionProvider;

        try {
            arrayOrCollectionProvider = inputs.get(0).getValue(dataContext);
        } catch (IllegalStateException e) {
            return false; // Input is missing, so it can't be "not empty".
        }

        // Check if the provided object is a Java array.
        if (arrayOrCollectionProvider.getClass().isArray()) {
            return Array.getLength(arrayOrCollectionProvider) > 0;
        }

        // Check if it's a Collection (like a List).
        if (arrayOrCollectionProvider instanceof Collection) {
            return !((Collection<?>) arrayOrCollectionProvider).isEmpty();
        }

        // If the input is of any other type, it's not a collection and the predicate is false.
        return false;
    }
}
