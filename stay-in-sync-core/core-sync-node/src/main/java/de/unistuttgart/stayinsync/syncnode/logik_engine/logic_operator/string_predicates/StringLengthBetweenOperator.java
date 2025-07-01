package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.string_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.InputNode;

import java.util.List;
import java.util.Map;

public class StringLengthBetweenOperator implements Operation {

    /**
     * Validates that the node has exactly three inputs: a string, a lower bound number,
     * and an upper bound number.
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node does not have exactly three inputs.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();
        if (inputs == null || inputs.size() != 3) {
            throw new IllegalArgumentException(
                    "STRING_LENGTH_BETWEEN operation for node '" + node.getNodeName() + "' requires exactly 3 inputs: the string, a lower bound, and an upper bound."
            );
        }
    }

    /**
     * Checks if a string's length falls inclusively between a lower and an upper bound.
     *
     * @param node        The LogicNode being evaluated. It expects a string and two numbers as inputs.
     * @param dataContext The runtime data context.
     * @return {@code true} if the string's length is >= lower bound and <= upper bound.
     * Returns {@code false} if inputs are missing or have incorrect types.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<InputNode> inputs = node.getInputProviders();
        Object stringProvider;
        Object lowerBoundProvider;
        Object upperBoundProvider;

        try {
            stringProvider = inputs.get(0).getValue(dataContext);
            lowerBoundProvider = inputs.get(1).getValue(dataContext);
            upperBoundProvider = inputs.get(2).getValue(dataContext);
        } catch (IllegalStateException e) {
            return false;
        }

        if (!(stringProvider instanceof String) || !(lowerBoundProvider instanceof Number) || !(upperBoundProvider instanceof Number)) {
            return false;
        }

        int stringLength = ((String) stringProvider).length();
        int lowerBound = ((Number) lowerBoundProvider).intValue();
        int upperBound = ((Number) upperBoundProvider).intValue();

        return stringLength >= lowerBound && stringLength <= upperBound;
    }
}