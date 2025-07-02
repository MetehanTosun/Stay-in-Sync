package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.datetime_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.InputNode;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Map;

public class BetweenDatesOperator implements Operation {

    /**
     * Validates that the node has exactly three inputs.
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node does not have exactly three inputs.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();
        if (inputs == null || inputs.size() != 3) {
            throw new IllegalArgumentException(
                    "BETWEEN_DATES operation requires exactly 3 inputs: the date to check, the start date, and the end date."
            );
        }
    }

    /**
     * Executes the comparison to check if a date falls inclusively between a start and an end date.
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the first date is on or after the second date AND on or before the third date.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<InputNode> inputs = node.getInputProviders();

        ZonedDateTime dateToCheck = toZonedDateTime(inputs.get(0).getValue(dataContext));
        ZonedDateTime startDate = toZonedDateTime(inputs.get(1).getValue(dataContext));
        ZonedDateTime endDate = toZonedDateTime(inputs.get(2).getValue(dataContext));

        if (dateToCheck == null || startDate == null || endDate == null) {
            return false;
        }

        // Check for (date >= start) AND (date <= end)
        // A robust way to write this is using isBefore/isAfter:
        boolean isOnOrAfterStart = !dateToCheck.isBefore(startDate);
        boolean isOnOrBeforeEnd = !dateToCheck.isAfter(endDate);

        return isOnOrAfterStart && isOnOrBeforeEnd;
    }

    /**
     * Converts an object into a ZonedDateTime, if possible.
     * This helper is copied here as this class does not inherit from the base class.
     * We assume UTC for simple date strings as per our design decision.
     */
    private ZonedDateTime toZonedDateTime(Object obj) {
        if (obj == null) return null;
        if (obj instanceof TemporalAccessor) {
            return ZonedDateTime.from((TemporalAccessor) obj);
        }
        if (obj instanceof String) {
            try {
                // This handles ISO 8601 format with timezone information.
                return ZonedDateTime.parse((String) obj);
            } catch (DateTimeParseException e) {
                // Fallback for simple date formats will be added if specified.
                return null;
            }
        }
        return null;
    }
}