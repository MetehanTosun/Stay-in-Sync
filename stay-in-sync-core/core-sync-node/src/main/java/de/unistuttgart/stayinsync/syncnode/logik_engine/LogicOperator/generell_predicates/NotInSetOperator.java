package de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.generell_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.ConstantNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.Operation;

import java.util.*;

public class NotInSetOperator implements Operation {


    /**
     * Validates that the LogicNode is correctly configured for the NOT_IN_SET operation.
     * <p>
     * This operation requires exactly two inputs:
     * <ol>
     *     <li>The first input provides the value to be checked.</li>
     *     <li>The second input must be a {@link ConstantNode} containing an
     *     array (e.g., {@code String[]}, {@code Object[]}) of disallowed values.</li>
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
                    "NOT_IN_SET operation for node '" + node.getNodeName() + "' requires exactly 2 inputs: the value to check, and a ConstantNode containing the array of disallowed values."
            );
        }

        if (!(inputs.get(1) instanceof ConstantNode)) {
            throw new IllegalArgumentException(
                    "NOT_IN_SET operation requires the second input (the array of disallowed values) to be a ConstantNode."
            );
        }

        ConstantNode setConstant = (ConstantNode) inputs.get(1);
        Object setValue = setConstant.getValue(null);

        if (setValue == null || !setValue.getClass().isArray()) {
            throw new IllegalArgumentException(
                    "The ConstantNode for NOT_IN_SET must contain an array (e.g., String[]), but got " + (setValue == null ? "null" : setValue.getClass().getSimpleName())
            );
        }
    }

    /**
     * Executes the set-exclusion check.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the value from the first input is **not** present in the
     *         array provided by the second input, otherwise {@code false}.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<InputNode> inputs = node.getInputProviders();

        InputNode valueToCheckProvider = inputs.get(0);
        Object valueToCheck;
        try {
            valueToCheck = valueToCheckProvider.getValue(dataContext);
        } catch (IllegalStateException e) {
            // If the value cannot be found, it cannot be in the (forbidden) set.
            // The result is also 'true'.
            return true;
        }

        ConstantNode setConstant = (ConstantNode) inputs.get(1);
        Object[] disallowedValuesArray = (Object[]) setConstant.getValue(dataContext);

        Set<Object> disallowedValuesSet = new HashSet<>(Arrays.asList(disallowedValuesArray));

        return !disallowedValuesSet.contains(valueToCheck);
    }
}
