package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.string_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.InputNode;

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
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();
        if (inputs == null || inputs.size() != 2) {
            throw new IllegalArgumentException(
                    "String comparison operation for node '" + node.getNodeName() + "' requires exactly 2 inputs."
            );
        }
    }

    /**
     * Orchestrates the execution by retrieving and validating two string inputs,
     * then delegating the actual comparison logic to the concrete subclass.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return A {@code Boolean} result from the comparison. Returns {@code false} if inputs
     * are missing or are not both strings.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<InputNode> inputs = node.getInputProviders();
        Object value1;
        Object value2;

        try {
            value1 = inputs.get(0).getValue(dataContext);
            value2 = inputs.get(1).getValue(dataContext);
        } catch (IllegalStateException e) {
            return false;
        }

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