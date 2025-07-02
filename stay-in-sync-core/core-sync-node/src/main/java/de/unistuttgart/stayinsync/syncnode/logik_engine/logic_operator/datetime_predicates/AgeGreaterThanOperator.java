package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.datetime_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.InputNode;

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
     * @throws IllegalArgumentException if the node does not have exactly three inputs.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();
        if (inputs == null || inputs.size() != 3) {
            throw new IllegalArgumentException(
                    "AGE_GREATER_THAN operation requires 3 inputs: a date-time, a numeric value, and a time unit string (e.g., 'DAYS')."
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
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<InputNode> inputs = node.getInputProviders();

        ZonedDateTime pastTimestamp = DateTimeParserUtil.toZonedDateTime(inputs.get(0).getValue(dataContext));
        Object thresholdProvider = inputs.get(1).getValue(dataContext);
        Object unitProvider = inputs.get(2).getValue(dataContext);

        if (pastTimestamp == null || !(thresholdProvider instanceof Number) || !(unitProvider instanceof String)) {
            return false;
        }

        long thresholdValue = ((Number) thresholdProvider).longValue();
        String unitString = ((String) unitProvider).toUpperCase();

        try {
            ChronoUnit unit = ChronoUnit.valueOf(unitString);
            ZonedDateTime now = ZonedDateTime.now(pastTimestamp.getZone()); // Calculate "now" in the same timezone for accuracy

            long age = unit.between(pastTimestamp, now);

            return age > thresholdValue;

        } catch (IllegalArgumentException e) {
            // This happens if the unitString is not a valid ChronoUnit enum name (e.g., "WEEKS", "DECADES")
            return false;
        }
    }
}