package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.datetime_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.exception.OperatorValidationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class WithinNextOperator implements Operation {

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
                    "WITHIN_NEXT operation for node '" + node.getName() + "' requires 3 inputs: a date-time, a numeric value, and a time unit string (e.g., 'DAYS')."
            );
        }
    }

    /**
     * Checks if a given date-time is within a future time window extending from now.
     * For example, "within the next 2 hours".
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the timestamp is within the specified future window.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<Node> inputs = node.getInputNodes();

        Object timestampValue = inputs.get(0).getCalculatedResult();
        Object valueProvider = inputs.get(1).getCalculatedResult();
        Object unitProvider = inputs.get(2).getCalculatedResult();

        // Use the utility to parse the first value into a ZonedDateTime.
        ZonedDateTime timestampToCheck = DateTimeParserUtil.toZonedDateTime(timestampValue);

        if (timestampToCheck == null || !(valueProvider instanceof Number) || !(unitProvider instanceof String)) {
            return false;
        }

        long value = ((Number) valueProvider).longValue();
        String unitString = ((String) unitProvider).toUpperCase();

        try {
            ChronoUnit unit = ChronoUnit.valueOf(unitString);
            // Calculate "now" in the same timezone as the input for accuracy.
            ZonedDateTime now = ZonedDateTime.now(timestampToCheck.getZone());
            ZonedDateTime windowEnd = now.plus(value, unit);

            // Check if timestamp is on or after now AND on or before the window end.
            boolean isOnOrAfterNow = !timestampToCheck.isBefore(now);
            boolean isOnOrBeforeWindowEnd = !timestampToCheck.isAfter(windowEnd);

            return isOnOrAfterNow && isOnOrBeforeWindowEnd;

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