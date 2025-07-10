package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.string_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.Node;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;

import java.util.List;
import java.util.Map;

/**
 * An abstract base class for operations that perform a comparison between two string inputs.
 * It uses the Template Method pattern to handle boilerplate code for input validation and retrieval.
 */
public abstract class AbstractTwoStringOperator implements Operation {

    /**
     * Validates that the node has exactly two inputs. This is a structural check.
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node does not have exactly two inputs.
     */
    @Override
    public void validateNode(LogicNode node) {
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.size() != 2) {
            throw new IllegalArgumentException(
                    "String comparison operation for node '" + node.getName() + "' requires exactly 2 inputs."
            );
        }
    }

    /**
     * Orchestrates the execution by retrieving and validating two string inputs,
     * then delegating the actual comparison logic to the concrete subclass.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return A {@code Boolean} result from the comparison. Returns {@code false} if any
     * provided value is null or not a string.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<Node> inputs = node.getInputNodes();

        Object value1 = inputs.get(0).getCalculatedResult();
        Object value2 = inputs.get(1).getCalculatedResult();

        // The comparison is only possible if both provided values are strings.
        if (!(value1 instanceof String) || !(value2 instanceof String)) {
            return false;
        }

        // Delegate the actual comparison to the subclass.
        return compareStrings((String) value1, (String) value2);
    }

    /**
     * Performs the specific string comparison logic. This method must be implemented
     * by concrete subclasses.
     *
     * @param str1 The first string input.
     * @param str2 The second string input.
     * @return The boolean result of the comparison.
     */
    protected abstract boolean compareStrings(String str1, String str2);
}