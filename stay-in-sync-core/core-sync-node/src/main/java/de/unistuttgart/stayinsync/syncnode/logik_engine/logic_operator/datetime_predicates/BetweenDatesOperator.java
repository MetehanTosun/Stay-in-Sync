package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.datetime_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.Node;

import java.time.ZonedDateTime;
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
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.size() != 3) {
            throw new IllegalArgumentException(
                    "BETWEEN_DATES operation for node '" + node.getName() + "' requires exactly 3 inputs: the date to check, the start date, and the end date."
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
        List<Node> inputs = node.getInputNodes();

        Object dateToCheckProvider = inputs.get(0).getCalculatedResult();
        Object startDateProvider = inputs.get(1).getCalculatedResult();
        Object endDateProvider = inputs.get(2).getCalculatedResult();

        // Use the central utility to parse all provided values into ZonedDateTime objects.
        ZonedDateTime dateToCheck = DateTimeParserUtil.toZonedDateTime(dateToCheckProvider);
        ZonedDateTime startDate = DateTimeParserUtil.toZonedDateTime(startDateProvider);
        ZonedDateTime endDate = DateTimeParserUtil.toZonedDateTime(endDateProvider);

        if (dateToCheck == null || startDate == null || endDate == null) {
            return false;
        }

        // Check for (date >= start) AND (date <= end)
        boolean isOnOrAfterStart = !dateToCheck.isBefore(startDate);
        boolean isOnOrBeforeEnd = !dateToCheck.isAfter(endDate);

        return isOnOrAfterStart && isOnOrBeforeEnd;
    }
}