package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.array_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.exception.OperatorValidationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;

import java.lang.reflect.Array;
import java.util.*;

/**
 * An abstract base class for operations that perform a set-based comparison
 * between two collection-like inputs (source and reference).
 */
public abstract class AbstractSetComparisonOperator implements Operation {

    /**
     * Validates that the node is correctly configured for a set-based comparison.
     * <p>
     * This check ensures that the operator is provided with exactly two input nodes.
     *
     * @param node The LogicNode to validate.
     * @throws OperatorValidationException if the node does not have exactly two inputs.
     */
    @Override
    public void validateNode(LogicNode node) {
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.size() != 2) {
            throw new OperatorValidationException(
                    "Set comparison operation for node '" + node.getName() + "' requires exactly 2 inputs: the source collection and the reference collection."
            );
        }
    }

    /**
     * Executes the set-based comparison using the Template Method design pattern.
     * <p>
     * This method orchestrates the execution flow:
     * <ol>
     * <li>It retrieves the pre-calculated results from its two input nodes.</li>
     * <li>It converts both results (which can be arrays or collections) into
     * standard {@link Collection} objects.</li>
     * <li><b>Performance Optimization:</b> It converts the source collection into a {@link java.util.HashSet}
     * to enable high-speed lookups with an average time complexity of O(1).</li>
     * <li>It then delegates the specific comparison logic to the
     * {@link #compareSets(Set, Collection)} method, which is implemented by the subclass.</li>
     * </ol>
     *
     * @param node        The LogicNode currently being evaluated.
     * @param dataContext The runtime data context.
     * @return A {@code Boolean} representing the result of the comparison. Returns {@code false}
     * if any input result is null or if the inputs are not collection-like types.
     * @see #compareSets(Set, Collection)
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<Node> inputs = node.getInputNodes();

        Object sourceProvider = inputs.get(0).getCalculatedResult();
        Object referenceProvider = inputs.get(1).getCalculatedResult();

        // If any upstream node failed to produce a result, the comparison is false.
        if (sourceProvider == null || referenceProvider == null) {
            return false;
        }

        Collection<?> sourceCollection = toCollection(sourceProvider);
        Collection<?> referenceCollection = toCollection(referenceProvider);

        if (sourceCollection == null || referenceCollection == null) {
            return false;
        }

        // --- PERFORMANCE OPTIMIZATION ---
        // Convert the source collection to a HashSet for O(1) average time complexity for contains().
        // This dramatically speeds up all set-based comparisons.
        Set<?> sourceSet = new HashSet<>(sourceCollection);

        // Delegate to the concrete implementation using the optimized Set
        return compareSets(sourceSet, referenceCollection);
    }

    /**
     * Converts a given object into a {@link Collection} if possible.
     * This helper method handles both arrays and existing Collection objects.
     */
    private static Collection<?> toCollection(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Collection) {
            return (Collection<?>) obj;
        }
        if (obj.getClass().isArray()) {
            List<Object> list = new ArrayList<>();
            int length = Array.getLength(obj);
            for (int i = 0; i < length; i++) {
                list.add(Array.get(obj, i));
            }
            return list;
        }
        return null;
    }

    /**
     * Performs the specific set-based comparison between the source and reference collections.
     * This method must be implemented by the concrete subclass.
     *
     * @param sourceSet           The source collection, pre-converted to a Set for high-performance lookups.
     * @param referenceCollection The collection containing the elements to check against.
     * @return The boolean result of the comparison.
     */
    protected abstract boolean compareSets(Set<?> sourceSet, Collection<?> referenceCollection);
}