package de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.array_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.Operation;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NotContainsElementOperator implements Operation {

    /**
     * Validates that the node has exactly two inputs: the array/collection and the element to check for absence.
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node does not have exactly two inputs.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();
        if (inputs == null || inputs.size() != 2) {
            throw new IllegalArgumentException(
                    "NOT_CONTAINS_ELEMENT operation for node '" + node.getNodeName() + "' requires exactly 2 inputs: the array/list and the element to search for."
            );
        }
    }

    /**
     * Executes the element absence check.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the first input (array/collection) does NOT contain the second input (element).
     *         Returns {@code false} if inputs are missing, the first input is not an array/collection,
     *         or if the element IS found.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<InputNode> inputs = node.getInputProviders();
        Object arrayOrCollectionProvider;
        Object elementToFind;

        try {
            arrayOrCollectionProvider = inputs.get(0).getValue(dataContext);
            elementToFind = inputs.get(1).getValue(dataContext);
        } catch (IllegalStateException e) {
            return false; // An input is missing, predicate cannot be true.
        }

        // Handle Collections (e.g., a List from a JSON array)
        if (arrayOrCollectionProvider instanceof Collection) {
            // Simply negate the result of 'contains'
            return !((Collection<?>) arrayOrCollectionProvider).contains(elementToFind);
        }

        // Handle native Java arrays
        if (arrayOrCollectionProvider.getClass().isArray()) {
            int length = Array.getLength(arrayOrCollectionProvider);
            for (int i = 0; i < length; i++) {
                Object currentElement = Array.get(arrayOrCollectionProvider, i);
                // If we find the element, the condition "not contains" is immediately false.
                if (Objects.equals(currentElement, elementToFind)) {
                    return false;
                }
            }
            // Loop finished without finding the element, so "not contains" is true.
            return true;
        }

        // The first input is not a type we can search in.
        return false;
    }
}
