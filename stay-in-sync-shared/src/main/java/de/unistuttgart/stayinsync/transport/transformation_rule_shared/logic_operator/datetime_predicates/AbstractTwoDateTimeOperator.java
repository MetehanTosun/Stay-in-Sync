package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.datetime_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * An abstract base class for operations that compare two date-time inputs.
 * It handles the validation, retrieval of pre-calculated results, and parsing of inputs into ZonedDateTime objects.
 */
public abstract class AbstractTwoDateTimeOperator implements Operation {

    /**
     * Validates that the node has exactly two inputs.
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node does not have exactly two inputs.
     */
    @Override
    public void validateNode(LogicNode node) {
        // We now access the list of parent Node objects.
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.size() != 2) {
            throw new IllegalArgumentException(
                    "Date-time comparison for node '" + node.getName() + "' requires exactly 2 inputs."
            );
        }
    }

    /**
     * Orchestrates the execution by retrieving the pre-calculated results of its inputs,
     * ensuring they can be parsed into ZonedDateTime objects, and then delegating the
     * actual comparison to the concrete subclass.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return A {@code Boolean} result. Returns {@code false} if results are null or cannot be parsed.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<Node> inputs = node.getInputNodes();

        Object dateTimeProvider1 = inputs.get(0).getCalculatedResult();
        Object dateTimeProvider2 = inputs.get(1).getCalculatedResult();

        // The DateTimeParserUtil is used to robustly convert the provided values
        ZonedDateTime dt1 = DateTimeParserUtil.toZonedDateTime(dateTimeProvider1);
        ZonedDateTime dt2 = DateTimeParserUtil.toZonedDateTime(dateTimeProvider2);

        if (dt1 == null || dt2 == null) {
            return false;
        }

        return compareDateTimes(dt1, dt2);
    }

    /**
     * Performs the specific date-time comparison logic.
     * This method must be implemented by the concrete subclass.
     *
     * @param dt1 The first date-time object.
     * @param dt2 The second date-time object.
     * @return The boolean result of the comparison.
     */
    protected abstract boolean compareDateTimes(ZonedDateTime dt1, ZonedDateTime dt2);
}