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

/**
 * An abstract base class for operations that compare two date-time inputs.
 * It handles the validation, retrieval, and parsing of inputs into ZonedDateTime objects.
 */
public abstract class AbstractTwoDateTimeOperator implements Operation {

    /**
     * Validates that the node has exactly two inputs.
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node does not have exactly two inputs.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();
        if (inputs == null || inputs.size() != 2) {
            throw new IllegalArgumentException(
                    "Date-time comparison for node '" + node.getNodeName() + "' requires exactly 2 inputs."
            );
        }
    }

    /**
     * Orchestrates the execution by parsing two inputs into ZonedDateTime objects
     * and then delegating the comparison to the concrete subclass.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return A {@code Boolean} result. Returns {@code false} if inputs cannot be parsed.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<InputNode> inputs = node.getInputProviders();
        ZonedDateTime dt1 = DateTimeParserUtil.toZonedDateTime(inputs.get(0).getValue(dataContext));
        ZonedDateTime dt2 = DateTimeParserUtil.toZonedDateTime(inputs.get(1).getValue(dataContext));

        if (dt1 == null || dt2 == null) {
            return false;
        }

        return compareDateTimes(dt1, dt2);
    }



    /**
     * Performs the specific date-time comparison logic.
     *
     * @param dt1 The first date-time object.
     * @param dt2 The second date-time object.
     * @return The boolean result of the comparison.
     */
    protected abstract boolean compareDateTimes(ZonedDateTime dt1, ZonedDateTime dt2);
}