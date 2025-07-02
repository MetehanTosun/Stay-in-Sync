package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.datetime_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.InputNode;

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
     * @throws IllegalArgumentException if the node does not have exactly three inputs.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();
        if (inputs == null || inputs.size() != 3) {
            throw new IllegalArgumentException(
                    "WITHIN_LAST operation requires 3 inputs: a date-time, a numeric value, and a time unit string (e.g., 'DAYS')."
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
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<InputNode> inputs = node.getInputProviders();

        ZonedDateTime timestampToCheck = DateTimeParserUtil.toZonedDateTime(inputs.get(0).getValue(dataContext));
        Object valueProvider = inputs.get(1).getValue(dataContext);
        Object unitProvider = inputs.get(2).getValue(dataContext);

        if (timestampToCheck == null || !(valueProvider instanceof Number) || !(unitProvider instanceof String)) {
            return false;
        }

        long value = ((Number) valueProvider).longValue();
        String unitString = ((String) unitProvider).toUpperCase();

        try {
            ChronoUnit unit = ChronoUnit.valueOf(unitString);
            ZonedDateTime now = ZonedDateTime.now(timestampToCheck.getZone());
            ZonedDateTime windowStart = now.minus(value, unit);

            // Check if timestamp is on or after the window start AND on or before now.
            boolean isOnOrAfterWindowStart = !timestampToCheck.isBefore(windowStart);
            boolean isOnOrBeforeNow = !timestampToCheck.isAfter(now);

            return isOnOrAfterWindowStart && isOnOrBeforeNow;

        } catch (IllegalArgumentException e) {
            return false; // Invalid time unit string
        }
    }
}