package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.array_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.Node;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;

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
    public void validateNode(LogicNode node) {
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.size() != 2) {
            throw new IllegalArgumentException(
                    "NOT_CONTAINS_ELEMENT operation for node '" + node.getName() + "' requires exactly 2 inputs: the array/list and the element to search for."
            );
        }
    }

    /**
     * Executes the element absence check on the pre-calculated results of its input nodes.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the first input (array/collection) does NOT contain the second input (element).
     * Returns {@code false} if the first input is null, not an array/collection, or if the element IS found.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<Node> inputs = node.getInputNodes();

        Object arrayOrCollectionProvider = inputs.get(0).getCalculatedResult();
        Object elementToFind = inputs.get(1).getCalculatedResult();

        // If the collection/array itself could not be resolved, we cannot prove the element is not in it.
        if (arrayOrCollectionProvider == null) {
            return false;
        }

        // Handle Collections
        if (arrayOrCollectionProvider instanceof Collection) {
            return !((Collection<?>) arrayOrCollectionProvider).contains(elementToFind);
        }

        // Handle native Java arrays
        if (arrayOrCollectionProvider.getClass().isArray()) {
            int length = Array.getLength(arrayOrCollectionProvider);
            for (int i = 0; i < length; i++) {
                Object currentElement = Array.get(arrayOrCollectionProvider, i);
                // If we find the element, the "not contains" condition is immediately false.
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

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}