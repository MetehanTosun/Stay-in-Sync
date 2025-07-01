package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.array_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * An abstract base class for operations that compare the length/size of an array or collection
 * against an expected numeric value.
 */
public abstract class AbstractArrayLengthComparisonOperator implements Operation {

    /**
     * Validates that the node has exactly two inputs: the array/collection and the expected length.
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node does not have exactly two inputs.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();
        if (inputs == null || inputs.size() != 2) {
            throw new IllegalArgumentException(
                    "Array length comparison for node '" + node.getNodeName() + "' requires exactly 2 inputs: the array/list and the expected length."
            );
        }
    }

    /**
     * Executes the length comparison.
     * <p>
     * It retrieves the two inputs, determines the length of the first input (if it's an array
     * or a collection), and then delegates the specific comparison logic to the
     * abstract {@link #compare(int, int)} method.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the comparison is successful. Returns {@code false} if inputs are
     *         missing, the first input is not an array/collection, or the second is not a number.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<InputNode> inputs = node.getInputProviders();
        Object arrayOrCollectionProvider;
        Object lengthProvider;

        try {
            arrayOrCollectionProvider = inputs.get(0).getValue(dataContext);
            lengthProvider = inputs.get(1).getValue(dataContext);
        } catch (IllegalStateException e) {
            return false; // An input is missing.
        }

        // The second input must be a number.
        if (!(lengthProvider instanceof Number)) {
            return false;
        }
        int expectedLength = ((Number) lengthProvider).intValue();

        // The first input must be an array or a collection.
        int actualLength;
        if (arrayOrCollectionProvider.getClass().isArray()) {
            actualLength = Array.getLength(arrayOrCollectionProvider);
        } else if (arrayOrCollectionProvider instanceof Collection) {
            actualLength = ((Collection<?>) arrayOrCollectionProvider).size();
        } else {
            // The provided input is not something we can get a length from.
            return false;
        }

        // Delegate the actual comparison to the concrete implementation.
        return compare(actualLength, expectedLength);
    }

    /**
     * Performs the specific comparison between the actual and expected length.
     *
     * @param actualLength   The determined length of the array or collection.
     * @param expectedLength The target length to compare against.
     * @return The boolean result of the comparison.
     */
    protected abstract boolean compare(int actualLength, int expectedLength);
}
