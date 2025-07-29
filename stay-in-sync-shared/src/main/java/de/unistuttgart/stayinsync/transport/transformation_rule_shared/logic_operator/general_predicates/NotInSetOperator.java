package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.exception.OperatorValidationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.ConstantNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;

import java.util.*;

public class NotInSetOperator implements Operation {

    /**
     * Validates that the LogicNode is correctly configured for the NOT_IN_SET operation.
     * <p>
     * This operation requires exactly two inputs:
     * <ol>
     * <li>The first input provides the value to be checked.</li>
     * <li>The second input must be a {@link ConstantNode} containing an
     * array of disallowed values.</li>
     * </ol>
     *
     * @param node The LogicNode to validate.
     * @throws OperatorValidationException if the node's configuration is invalid.
     */
    @Override
    public void validateNode(LogicNode node) {
        List<Node> inputs = node.getInputNodes();

        if (inputs == null || inputs.size() != 2) {
            throw new OperatorValidationException(
                    "NOT_IN_SET operation for node '" + node.getName() + "' requires exactly 2 inputs: the value to check, and a ConstantNode with the array of values."
            );
        }

        Node setInputNode = inputs.get(1);
        if (!(setInputNode instanceof ConstantNode)) {
            throw new OperatorValidationException(
                    "NOT_IN_SET operation requires the second input to be a ConstantNode."
            );
        }

        // Check the configured value inside the ConstantNode directly.
        Object setValue = ((ConstantNode) setInputNode).getValue();

        if (setValue == null || !setValue.getClass().isArray()) {
            throw new OperatorValidationException(
                    "The ConstantNode for NOT_IN_SET must contain an array, but got " + (setValue == null ? "null" : setValue.getClass().getSimpleName())
            );
        }
    }

    /**
     * Executes the set-exclusion check on the pre-calculated values of its inputs.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the value from the first input is **not** present in the
     * array provided by the second input, otherwise {@code false}.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<Node> inputs = node.getInputNodes();

        Object valueToCheck = inputs.get(0).getCalculatedResult();
        Object disallowedValuesObject = inputs.get(1).getCalculatedResult();

        // If the set is invalid, we cannot make a determination and return false.
        if (!(disallowedValuesObject instanceof Object[])) {
            return false;
        }
        Object[] disallowedValuesArray = (Object[]) disallowedValuesObject;

        // 3. Perform the check using a Set for efficient lookup.
        Set<Object> disallowedValuesSet = new HashSet<>(Arrays.asList(disallowedValuesArray));

        // The '!disallowedValuesSet.contains()' logic correctly handles all cases,
        return !disallowedValuesSet.contains(valueToCheck);
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}