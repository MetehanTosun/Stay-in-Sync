package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.exception.OperatorValidationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;

import java.util.List;
import java.util.Map;

public class OneOfOperator implements Operation {
    /**
     * Validates that the LogicNode has at least one input node configured.
     * <p>
     * This operation requires at least one input to perform the logical OR evaluation.
     * </p>
     *
     * @param node The LogicNode to validate
     * @throws OperatorValidationException if the node has no input nodes configured
     */
    @Override
    public void validateNode(LogicNode node) {
        List<Node> inputs = node.getInputNodes();
        if(inputs == null || inputs.isEmpty()){
            throw new OperatorValidationException("ONE_OF operation is for node" + node.getName() + "requires at least 1 input");
        }
    }

    /**
     * Executes the logical OR operation on all input nodes.
     * <p>
     * Iterates through all input nodes and returns {@code true} as soon as any node
     *  has a calculated value of {@code Boolean.TRUE}. Non-true values (including null)
     *  are simply skipped. If a node throws an {@link IllegalStateException},
     * it is skipped and processing continues with the next node.
     * </p>
     *
     * @param node The LogicNode being evaluated
     * @param dataContext The runtime data context containing variable values
     * @return {@code true} if at least one input node evaluates to {@code true},
     *         {@code false} if all nodes evaluate to {@code false} or throw exceptions
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext){
        for(Node input: node.getInputNodes()){
            Object value = input.getCalculatedResult();

            if(Boolean.TRUE.equals(value)){
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
