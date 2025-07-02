package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.datetime_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.InputNode;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * An abstract base class for operations that compare a date-time object against a numeric value.
 */
public abstract class AbstractDateTimeNumericOperator implements Operation {

    /**
     * Validates that the node has exactly two inputs: a date-time and a number.
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node does not have exactly two inputs.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();
        if (inputs == null || inputs.size() != 2) {
            throw new IllegalArgumentException(
                    "Date-time to number comparison for node '" + node.getNodeName() + "' requires exactly 2 inputs: a date-time and a number."
            );
        }
    }

    /**
     * Orchestrates the execution by parsing the date-time, retrieving the number,
     * and then delegating the actual comparison logic to the concrete subclass.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return A {@code Boolean} result. Returns {@code false} if inputs are missing or have incorrect types.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<InputNode> inputs = node.getInputProviders();

        ZonedDateTime dateTime = DateTimeParserUtil.toZonedDateTime(inputs.get(0).getValue(dataContext));
        Object numberProvider = inputs.get(1).getValue(dataContext);

        if (dateTime == null || !(numberProvider instanceof Number)) {
            return false;
        }

        int comparisonValue = ((Number) numberProvider).intValue();

        return compareDateTimeAndNumber(dateTime, comparisonValue);
    }

    /**
     * Performs the specific comparison between the date-time's property and the numeric value.
     * @param dateTime The date-time object to extract a value from.
     * @param comparisonValue The numeric value to compare against.
     * @return The boolean result of the comparison.
     */
    protected abstract boolean compareDateTimeAndNumber(ZonedDateTime dateTime, int comparisonValue);
}