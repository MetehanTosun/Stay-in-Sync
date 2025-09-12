package de.unistuttgart.graphengine.logic_operator.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.Operation;
import de.unistuttgart.graphengine.nodes.ConstantNode;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;

import java.util.*;

public class InSetOperator implements Operation {

    /**
     * Validates that the LogicNode is correctly configured for the IN_SET operation.
     * <p>
     * This operation requires exactly two inputs:
     * <ol>
     * <li>The first input provides the value to be checked.</li>
     * <li>The second input must be a {@link ConstantNode} containing an
     * array of allowed values.</li>
     * </ol>
     *
     * @param node The LogicNode to validate.
     * @throws OperatorValidationException if the node's configuration is invalid.
     */
    @Override
    public void validateNode(LogicNode node)throws OperatorValidationException {
        List<Node> inputs = node.getInputNodes();

        if (inputs == null || inputs.size() != 2) {
            throw new OperatorValidationException(
                    "IN_SET operation for node '" + node.getName() + "' requires exactly 2 inputs: the value to check, and a ConstantNode with the array of values."
            );
        }

        Node setInputNode = inputs.get(1);
        if (!(setInputNode instanceof ConstantNode)) {
            throw new OperatorValidationException(
                    "IN_SET operation requires the second input to be a ConstantNode."
            );
        }

        // Check the configured value inside the ConstantNode directly.
        Object setValue = ((ConstantNode) setInputNode).getValue();

        if (setValue == null || !setValue.getClass().isArray()) {
            throw new OperatorValidationException(
                    "The ConstantNode for IN_SET must contain an array, but got " + (setValue == null ? "null" : setValue.getClass().getSimpleName())
            );
        }
    }

    /**
     * Executes the set-membership check.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the value from the first input is present in the
     * array provided by the second input, otherwise {@code false}.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<Node> inputs = node.getInputNodes();

        // 1. Get the pre-calculated value to check from the first input node.
        Object valueToCheck = inputs.get(0).getCalculatedResult();

        // 2. Get the pre-calculated array of allowed values from the second input node.
        Object allowedValuesObject = inputs.get(1).getCalculatedResult();

        // A null check is added for safety. The validate() method already ensures it's an array.
        if (!(allowedValuesObject instanceof Object[])) {
            return false;
        }
        Object[] allowedValuesArray = (Object[]) allowedValuesObject;

        // 3. Perform the check using a Set for efficient lookup.
        Set<Object> allowedValuesSet = new HashSet<>(Arrays.asList(allowedValuesArray));

        return allowedValuesSet.contains(valueToCheck);
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}