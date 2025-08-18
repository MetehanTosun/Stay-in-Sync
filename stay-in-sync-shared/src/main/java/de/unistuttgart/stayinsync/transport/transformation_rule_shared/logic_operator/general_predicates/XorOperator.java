package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.exception.GraphEvaluationException;
import de.unistuttgart.stayinsync.transport.exception.OperatorValidationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;

import java.util.List;
import java.util.Map;

public class XorOperator implements Operation {

    /**
     * Validates that the LogicNode is correctly configured for the XOR operation.
     * <p>
     * This operation requires at least two inputs to perform a meaningful XOR.
     *
     * @param node The LogicNode to validate.
     * @throws OperatorValidationException if the node does not have at least two inputs.
     */
    @Override
    public void validateNode(LogicNode node)throws OperatorValidationException {
        List<Node> inputs = node.getInputNodes();

        if (inputs == null || inputs.size() < 2) {
            throw new OperatorValidationException(
                    "XOR operation for node '" + node.getName() + "' requires at least 2 inputs."
            );
        }
    }

    /**
     * Executes the "exactly one is true" check on all boolean inputs.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if exactly one input resolves to {@code true}, otherwise {@code false}.
     * @throws GraphEvaluationException if any resolved input value is not a Boolean.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) throws GraphEvaluationException {
        int count = 0;

        for (Node inputNode : node.getInputNodes()) {
            Object value = inputNode.getCalculatedResult();

            if(value == null) {
                continue;
            }

            if (!(value instanceof Boolean)) {
                throw new GraphEvaluationException(
                        GraphEvaluationException.ErrorType.TYPE_MISMATCH,
                        "Type Mismatch in XOR",
                        "XOR operation for node '" + node.getName() + "' requires boolean inputs, but got a " + (value == null ? "null" : value.getClass().getSimpleName()),
                        null
                );
            }

            if(Boolean.TRUE.equals(value)){
                count++;
            }

            if(count > 1){
                return false;
            }
        }

        if(count == 1){
            return true;
        }
        return false;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}
