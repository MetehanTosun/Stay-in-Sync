package de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.array_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.Operation;

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
     * This check is purely structural and ensures that the operator is provided with the
     * correct number of inputs (arity) before execution. It requires exactly two inputs:
     * a source collection and a reference collection.
     *
     * @param node The LogicNode to validate. It must contain a list of input providers.
     * @throws IllegalArgumentException if the node does not have exactly two inputs,
     *                                  which would make a subsequent comparison impossible.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();
        if (inputs == null || inputs.size() != 2) {
            throw new IllegalArgumentException(
                    "Set comparison operation for node '" + node.getNodeName() + "' requires exactly 2 inputs: the source collection and the reference collection."
            );
        }
    }

    /**
     * Executes the set-based comparison using the Template Method design pattern.
     * <p>
     * This method orchestrates the entire execution flow:
     * <ol>
     *     <li>It retrieves the values from the two input providers.</li>
     *     <li>It converts both input values (which can be arrays or collections) into
     *         standard {@link Collection} objects.</li>
     *     <li><b>Performance Optimization:</b> It converts the source collection into a {@link java.util.HashSet}
     *         to enable high-speed lookups with an average time complexity of O(1).</li>
     *     <li>It then delegates the final, specific comparison logic (e.g., containsAll, containsAny)
     *         to the {@link #compareSets(Set, Collection)} method, which must be implemented
     *         by the concrete subclass.</li>
     * </ol>
     *
     * @param node        The LogicNode currently being evaluated.
     * @param dataContext The runtime data context, used to resolve inputs from JSON sources.
     * @return A {@code Boolean} representing the result of the comparison. Returns {@code false}
     *         if any input cannot be retrieved or if the inputs are not collection-like types.
     * @see #compareSets(Set, Collection)
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<InputNode> inputs = node.getInputProviders();
        Object sourceProvider;
        Object referenceProvider;

        try {
            sourceProvider = inputs.get(0).getValue(dataContext);
            referenceProvider = inputs.get(1).getValue(dataContext);
        } catch (IllegalStateException e) {
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
     */
    private static Collection<?> toCollection(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Collection) return (Collection<?>) obj;
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
     *
     * @param sourceSet           The source collection, pre-converted to a Set for high-performance lookups.
     * @param referenceCollection The collection containing the elements to check against.
     * @return The boolean result of the comparison.
     */
    protected abstract boolean compareSets(Set<?> sourceSet, Collection<?> referenceCollection);
}