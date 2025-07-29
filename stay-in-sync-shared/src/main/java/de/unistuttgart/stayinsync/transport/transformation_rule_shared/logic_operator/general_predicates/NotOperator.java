package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.exception.GraphEvaluationException;
import de.unistuttgart.stayinsync.transport.exception.OperatorValidationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;

import java.util.List;
import java.util.Map;

public class NotOperator implements Operation {

    /**
     * Validates that the LogicNode is correctly configured for the NOT operation.
     * <p>
     * This operation requires exactly one input.
     *
     * @param node The LogicNode to validate.
     * @throws OperatorValidationException if the node does not have exactly one input.
     */
    @Override
    public void validateNode(LogicNode node) {
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.size() != 1) {
            throw new OperatorValidationException(
                    "NOT operation for node '" + node.getName() + "' requires exactly 1 input, but got " + (inputs == null ? 0 : inputs.size())
            );
        }
    }

    /**
     * Executes the logical negation.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return The negated boolean value of the input.
     * @throws GraphEvaluationException if the resolved input value is not a Boolean.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) throws GraphEvaluationException {
        Node inputNode = node.getInputNodes().get(0);

        Object value = inputNode.getCalculatedResult();

        // Runtime type validation: The value must be a boolean.
        if (!(value instanceof Boolean)) {
            throw new GraphEvaluationException(
                    GraphEvaluationException.ErrorType.TYPE_MISMATCH,
                    "Type Mismatch in NOT",
                    "NOT operation for node '" + node.getName() + "' requires a boolean input, but got a " + (value == null ? "null" : value.getClass().getSimpleName()),
                    null
            );
        }
        return !((Boolean) value);
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}
