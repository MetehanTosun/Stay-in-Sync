package de.unistuttgart.graphengine.logic_operator.datetime_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.Operation;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class WithinLastOperator implements Operation {

    /**
     * Validates that the node has exactly three inputs: a date-time, a numeric value,
     * and a string representing the time unit.
     *
     * @param node The LogicNode to validate.
     * @throws OperatorValidationException if the node does not have exactly three inputs.
     */
    @Override
    public void validateNode(LogicNode node)throws OperatorValidationException {
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.size() != 3) {
            throw new OperatorValidationException(
                    "WITHIN_LAST operation for node '" + node.getName() + "' requires 3 inputs: a date-time, a numeric value, and a time unit string (e.g., 'DAYS')."
            );
        }
    }

    /**
     * Checks if a given date-time is within a recent time window extending from now into the past.
     * For example, "within the last 7 days".
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the timestamp is within the specified window.
     */
    @Override
    public Object execute(LogicNode node, Map<String, Object> dataContext) {
        List<Node> inputs = node.getInputNodes();

        Object timestampProvider = inputs.get(0).getCalculatedResult();
        Object valueProvider = inputs.get(1).getCalculatedResult();
        Object unitProvider = inputs.get(2).getCalculatedResult();

        // // Use the central utility to parse all provided values into ZonedDateTime objects.
        ZonedDateTime timestampToCheck = DateTimeParserUtil.toZonedDateTime(timestampProvider);

        if (timestampToCheck == null || !(valueProvider instanceof Number) || !(unitProvider instanceof String)) {
            return false;
        }

        long value = ((Number) valueProvider).longValue();
        String unitString = ((String) unitProvider).toUpperCase();

        try {
            ChronoUnit unit = ChronoUnit.valueOf(unitString);
            // Calculate "now" in the same timezone as the input for accuracy.
            ZonedDateTime now = ZonedDateTime.now(timestampToCheck.getZone());
            ZonedDateTime windowStart = now.minus(value, unit);

            // Check if timestamp is on or after the window start AND on or before now.
            boolean isOnOrAfterWindowStart = !timestampToCheck.isBefore(windowStart);
            boolean isOnOrBeforeNow = !timestampToCheck.isAfter(now);

            return isOnOrAfterWindowStart && isOnOrBeforeNow;

        } catch (IllegalArgumentException e) {
            // This happens if the unitString is not a valid ChronoUnit enum name.
            return false;
        }
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}