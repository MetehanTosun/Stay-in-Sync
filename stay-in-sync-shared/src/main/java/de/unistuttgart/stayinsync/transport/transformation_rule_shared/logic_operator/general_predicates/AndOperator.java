package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.exception.GraphEvaluationException;
import de.unistuttgart.stayinsync.transport.exception.OperatorValidationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;

import java.util.List;
import java.util.Map;

public class AndOperator implements Operation {

    /**
     * Validates that the LogicNode is correctly configured for the AND operation.
     * <p>
     * This operation requires at least two input nodes.
     *
     * @param node The LogicNode to validate.
     * @throws OperatorValidationException if the node does not have at least two inputs.
     */
    @Override
    public void validateNode(LogicNode node) {
        List<Node> inputs = node.getInputNodes();
        if (inputs == null || inputs.size() < 2) {
            throw new OperatorValidationException(
                    "AND operation for node '" + node.getName() + "' requires at least 2 inputs."
            );
        }
    }

    /**
     * Executes a short-circuit boolean AND operation on all input values.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if all provided input values are true, otherwise {@code false}.
     * @throws GraphEvaluationException if any provided input value is not a Boolean.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) throws GraphEvaluationException {
        List<Node> inputs = node.getInputNodes();

        for (Node inputNode : inputs) {
            Object value = inputNode.getCalculatedResult();

            // --- RUNTIME TYPE VALIDATION ---
            // This strict check is a core requirement of the AND operator.
            // It ensures that only boolean values are being combined.
            if (!(value instanceof Boolean)) {
                throw new GraphEvaluationException(
                        GraphEvaluationException.ErrorType.TYPE_MISMATCH,
                        "Type Mismatch in AND",
                        "AND operation for node '" + node.getName() + "' requires boolean inputs, but got a " + (value == null ? "null" : value.getClass().getSimpleName()),
                        null
                );
            }

            // If any input is false, the entire AND expression is false (short-circuit).
            if (!((Boolean) value)) {
                return false;
            }
        }

        // If the loop completes, all inputs were true.
        return true;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}