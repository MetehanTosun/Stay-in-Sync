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

public class ContainsElementOperator implements Operation {

    /**
     * Validates that the node has exactly two inputs: the array/collection and the element to find.
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node does not have exactly two inputs.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();
        if (inputs == null || inputs.size() != 2) {
            throw new IllegalArgumentException(
                    "CONTAINS_ELEMENT operation for node '" + node.getNodeName() + "' requires exactly 2 inputs: the array/list and the element to search for."
            );
        }
    }

    /**
     * Executes the element containment check.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the first input (array/collection) contains the second input (element).
     *         Returns {@code false} if inputs are missing, the first input is not an array/collection,
     *         or the element is not found.
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
            return false; // An input is missing.
        }

        // Handle Collections (e.g., a List from a JSON array)
        if (arrayOrCollectionProvider instanceof Collection) {
            return ((Collection<?>) arrayOrCollectionProvider).contains(elementToFind);
        }

        // Handle native Java arrays
        if (arrayOrCollectionProvider.getClass().isArray()) {
            int length = Array.getLength(arrayOrCollectionProvider);
            for (int i = 0; i < length; i++) {
                Object currentElement = Array.get(arrayOrCollectionProvider, i);
                // Use Objects.equals for safe comparison (handles nulls correctly)
                if (Objects.equals(currentElement, elementToFind)) {
                    return true;
                }
            }
            return false; // Loop finished, element not found.
        }

        // The first input is not a type we can search in.
        return false;
    }
}