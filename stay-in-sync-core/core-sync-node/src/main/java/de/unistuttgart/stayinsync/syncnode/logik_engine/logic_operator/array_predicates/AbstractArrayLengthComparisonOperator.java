package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.array_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.Node; // Import the base class
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
    public void validateNode(LogicNode node) {
        // We now access the list of parent Node objects.
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.size() != 2) {
            throw new IllegalArgumentException(
                    "Array length comparison for node '" + node.getName() + "' requires exactly 2 inputs: the array/list and the expected length."
            );
        }
    }

    /**
     * Executes the length comparison.
     * <p>
     * It retrieves the pre-calculated results of its two inputs, determines the length
     * of the first input, and then delegates the specific comparison logic to the
     * abstract {@link #compare(int, int)} method.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context (passed down but often not used here).
     * @return {@code true} if the comparison is successful. Returns {@code false} if results
     * are null, or if types are incorrect.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<Node> inputs = node.getInputNodes();

        Object arrayOrCollectionProvider = inputs.get(0).getCalculatedResult();
        Object lengthProvider = inputs.get(1).getCalculatedResult();

        // A simple null check replaces the try-catch block.
        if (arrayOrCollectionProvider == null || lengthProvider == null) {
            return false;
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
            return false;
        }

        // Delegate the actual comparison to the concrete implementation. This does not change.
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