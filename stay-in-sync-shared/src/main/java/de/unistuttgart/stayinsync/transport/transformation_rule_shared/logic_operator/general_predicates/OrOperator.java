package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.exception.GraphEvaluationException;
import de.unistuttgart.stayinsync.transport.exception.OperatorValidationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;

import java.util.List;
import java.util.Map;

public class OrOperator implements Operation {


    /**
     * Validates that the LogicNode is correctly configured for the OR operation.
     * <p>
     * This operation requires at least two inputs to perform a logical OR.
     *
     * @param node The LogicNode to validate.
     * @throws OperatorValidationException if the node does not have at least two inputs.
     */
    public void validateNode(LogicNode node) {
        List<Node> inputs = node.getInputNodes();

        if (inputs == null || inputs.size() < 2) {
            throw new OperatorValidationException(
                    "OR operation for node '" + node.getName() + "' requires at least 2 inputs."
            );
        }
    }

    /**
     * Executes a short-circuit boolean OR operation on all input values.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if at least one resolved input value is true, otherwise {@code false}.
     * @throws GraphEvaluationException if any resolved input value is not a Boolean.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) throws GraphEvaluationException {
        for (Node inputProvider : node.getInputNodes()) {
            Object value = inputProvider.getCalculatedResult();

            if (!(value instanceof Boolean)) {
                throw new GraphEvaluationException(
                        GraphEvaluationException.ErrorType.TYPE_MISMATCH,
                        "Type Mismatch in OR",
                        "OR operation for node '" + node.getName() + "' requires boolean inputs, but got a " + (value == null ? "null" : value.getClass().getSimpleName()),
                        null
                );
            }

            if (((Boolean) value)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}
