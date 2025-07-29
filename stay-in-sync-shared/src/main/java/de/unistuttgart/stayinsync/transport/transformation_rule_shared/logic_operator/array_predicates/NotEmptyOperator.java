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

public class NotEmptyOperator implements Operation {

    /**
     * Validates that the node has exactly one input, which is the array or collection to check.
     *
     * @param node The LogicNode to validate.
     * @throws OperatorValidationException if the node does not have exactly one input.
     */
    @Override
    public void validateNode(LogicNode node) {
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.size() != 1) {
            throw new OperatorValidationException(
                    "NOT_EMPTY operation for node '" + node.getName() + "' requires exactly 1 input: the array or collection to check."
            );
        }
    }

    /**
     * Executes the not-empty check on the pre-calculated result of its input node.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the input is an array or collection with at least one element.
     * Returns {@code false} if the input is null, not an array/collection, or is empty.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        Node inputNode = node.getInputNodes().get(0);

        Object arrayOrCollectionProvider = inputNode.getCalculatedResult();

        // If the upstream node failed to produce a result, it cannot be "not empty".
        if (arrayOrCollectionProvider == null) {
            return false;
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

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}