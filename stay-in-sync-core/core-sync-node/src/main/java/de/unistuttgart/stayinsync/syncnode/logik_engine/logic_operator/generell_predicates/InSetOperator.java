package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.generell_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.ConstantNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;

import java.util.*;

public class InSetOperator implements Operation {

    /**
     * Validates that the LogicNode is correctly configured for the IN_SET operation.
     * <p>
     * This operation requires exactly two inputs:
     * <ol>
     *     <li>The first input provides the value to be checked.</li>
     *     <li>The second input must be a {@link ConstantNode} containing an
     *     array (e.g., {@code String[]}, {@code Object[]}) of allowed values.</li>
     * </ol>
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node's configuration is invalid.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();

        if (inputs == null || inputs.size() != 2) {
            throw new IllegalArgumentException(
                    "IN_SET operation for node '" + node.getNodeName() + "' requires exactly 2 inputs: the value to check, and a ConstantNode containing the array of allowed values."
            );
        }

        if (!(inputs.get(1) instanceof ConstantNode)) {
            throw new IllegalArgumentException(
                    "IN_SET operation requires the second input (the array of allowed values) to be a ConstantNode."
            );
        }

        ConstantNode setConstant = (ConstantNode) inputs.get(1);
        Object setValue = setConstant.getValue();

        if (setValue == null || !setValue.getClass().isArray()) {
            throw new IllegalArgumentException(
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
     *         array provided by the second input, otherwise {@code false}.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<InputNode> inputs = node.getInputProviders();

        // 1. Get the value to check from the FIRST input.
        InputNode valueToCheckProvider = inputs.get(0);
        Object valueToCheck;
        try {
            valueToCheck = valueToCheckProvider.getValue(dataContext);
        } catch (IllegalStateException e) {
            return false;
        }

        // 2. Get the array of allowed values from the SECOND input.
        // Thanks to validate() we know this is safe.
        ConstantNode setConstant = (ConstantNode) inputs.get(1);
        Object[] allowedValuesArray = (Object[]) setConstant.getValue(dataContext);

        // 3. Perform the check.
        // We convert the array to a Set for efficient 'contains' checking.
        Set<Object> allowedValuesSet = new HashSet<>(Arrays.asList(allowedValuesArray));

        return allowedValuesSet.contains(valueToCheck);
    }
}
