package de.unistuttgart.graphengine.logic_operator.datetime_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.Operation;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class AgeGreaterThanOperator implements Operation {

    /**
     * Validates that the node has exactly three inputs: a date-time, a numeric threshold,
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
                    "AGE_GREATER_THAN operation for node '" + node.getName() + "' requires 3 inputs: a date-time, a numeric value, and a time unit string (e.g., 'DAYS')."
            );
        }
    }

    /**
     * Checks if the duration between a past date-time and the current time is greater than a given threshold.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the age is greater than the specified amount in the given unit.
     */
    @Override
    public Object execute(LogicNode node, Map<String, Object> dataContext) {
        List<Node> inputs = node.getInputNodes();

        Object pastTimestampProvider = inputs.get(0).getCalculatedResult();
        Object thresholdProvider = inputs.get(1).getCalculatedResult();
        Object unitProvider = inputs.get(2).getCalculatedResult();

        // Use the central utility to parse all provided values into ZonedDateTime objects.
        ZonedDateTime pastTimestamp = DateTimeParserUtil.toZonedDateTime(pastTimestampProvider);

        if (pastTimestamp == null || !(thresholdProvider instanceof Number) || !(unitProvider instanceof String)) {
            return false;
        }

        long thresholdValue = ((Number) thresholdProvider).longValue();
        String unitString = ((String) unitProvider).toUpperCase();

        try {
            ChronoUnit unit = ChronoUnit.valueOf(unitString);
            // Calculate "now" in the same timezone as the input for accuracy.
            ZonedDateTime now = ZonedDateTime.now(pastTimestamp.getZone());

            long age = unit.between(pastTimestamp, now);

            return age > thresholdValue;

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