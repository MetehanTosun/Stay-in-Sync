package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.generell_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;

import java.util.List;
import java.util.Map;

public class NoneOfOperator implements Operation {

    /**
     * Validates that the LogicNode is correctly configured for the NONE_OF operation.
     * <p>
     * This operation requires at least one input to evaluate.
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node has no inputs.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException(
                    "ALL_OF operation for node '" + node.getNodeName() + "' requires at least 1 input."
            );
        }
    }

    /**
     * Executes the strict "none are true" check.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} only if all inputs resolve to something other than
     *         {@code Boolean.TRUE}, otherwise returns {@code false}.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        for (InputNode inputProvider : node.getInputProviders()) {
            Object value;
            try {
                value = inputProvider.getValue(dataContext);
            } catch (IllegalStateException e) {
                // An unresolvable value is not 'true'.
                continue;
            }

            // Strict check: The value must not only be a Boolean,
            // it must be the Boolean value 'true'.

            // We use Boolean.TRUE to ensure that we don't accidentally
            // compare a string "true" with a Boolean true.
            if (Boolean.TRUE.equals(value)) {
                return false;
            }
        }

        return true;
    }
}
