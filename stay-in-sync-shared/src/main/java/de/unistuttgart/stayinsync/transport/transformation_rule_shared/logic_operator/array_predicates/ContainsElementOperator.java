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
import java.util.Objects;

public class ContainsElementOperator implements Operation {

    /**
     * Validates that the node has exactly two inputs: the array/collection and the element to find.
     *
     * @param node The LogicNode to validate.
     * @throws OperatorValidationException if the node does not have exactly two inputs.
     */
    @Override
    public void validateNode(LogicNode node) {
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.size() != 2) {
            throw new OperatorValidationException(
                    "CONTAINS_ELEMENT operation for node '" + node.getName() + "' requires exactly 2 inputs: the array/list and the element to search for."
            );
        }
    }

    /**
     * Executes the element containment check.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the first input (array/collection) contains the second input (element).
     * Returns {@code false} if the first input is null, not an array/collection,
     * or the element is not found.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<Node> inputs = node.getInputNodes();

        Object arrayOrCollectionProvider = inputs.get(0).getCalculatedResult();
        Object elementToFind = inputs.get(1).getCalculatedResult();

        // If the collection/array itself could not be resolved, it cannot contain the element.
        if (arrayOrCollectionProvider == null) {
            return false;
        }

        // Handle Collections (e.g., a List from a JSON array).
        if (arrayOrCollectionProvider instanceof Collection) {
            return ((Collection<?>) arrayOrCollectionProvider).contains(elementToFind);
        }

        // Handle native Java arrays.
        if (arrayOrCollectionProvider.getClass().isArray()) {
            int length = Array.getLength(arrayOrCollectionProvider);
            for (int i = 0; i < length; i++) {
                Object currentElement = Array.get(arrayOrCollectionProvider, i);
                // Use Objects.equals for safe comparison (handles nulls correctly).
                if (Objects.equals(currentElement, elementToFind)) {
                    return true;
                }
            }
            return false; // Loop finished, element not found.
        }

        // The first input is not a type we can search in.
        return false;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}