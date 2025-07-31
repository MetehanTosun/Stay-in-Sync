package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.string_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.exception.OperatorValidationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;

import java.util.List;
import java.util.Map;

/**
 * An abstract base class for operations that compare the length of a string against a numeric value.
 */
public abstract class AbstractStringLengthComparisonOperator implements Operation {

    /**
     * Validates that the node has exactly two inputs: a string and a number.
     *
     * @param node The LogicNode to validate.
     * @throws OperatorValidationException if the node does not have exactly two inputs.
     */
    @Override
    public void validateNode(LogicNode node)throws OperatorValidationException {
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.size() != 2) {
            throw new OperatorValidationException(
                    "String length comparison for node '" + node.getName() + "' requires exactly 2 inputs: the string and a number."
            );
        }
    }

    /**
     * Orchestrates the execution by getting the string's length and the numeric value,
     * then delegating the comparison to the concrete subclass.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return A {@code Boolean} result. Returns {@code false} if inputs are null or have incorrect types.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<Node> inputs = node.getInputNodes();

        Object stringProvider = inputs.get(0).getCalculatedResult();
        Object numberProvider = inputs.get(1).getCalculatedResult();

        if (!(stringProvider instanceof String) || !(numberProvider instanceof Number)) {
            return false;
        }

        int stringLength = ((String) stringProvider).length();
        int comparisonValue = ((Number) numberProvider).intValue();

        return compareLength(stringLength, comparisonValue);
    }

    /**
     * Performs the specific length comparison logic.
     *
     * @param length The actual length of the input string.
     * @param comparisonValue  The numeric value to compare against.
     * @return The boolean result of the comparison.
     */
    protected abstract boolean compareLength(int length, int comparisonValue);
}