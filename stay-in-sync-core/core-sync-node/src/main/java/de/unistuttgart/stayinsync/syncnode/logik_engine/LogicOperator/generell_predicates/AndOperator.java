package de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.generell_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.Operation;

import java.util.List;
import java.util.Map;

public class AndOperator implements Operation {

    /**
     * Validates that the LogicNode is correctly configured for the AND operation.
     * <p>
     * This operation requires at least two inputs. The specific runtime type
     * of the inputs (e.g., boolean) is checked during execution.
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node does not have at least two inputs.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();

        if (inputs == null || inputs.size() < 2) {
            throw new IllegalArgumentException(
                    "AND operation for node '" + node.getNodeName() + "' requires at least 2 inputs."
            );
        }
    }

    /**
     * Executes a short-circuit boolean AND operation on all input values.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if all resolved input values are true, otherwise {@code false}.
     * @throws IllegalArgumentException if any resolved input value is not a Boolean.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        for (InputNode inputProvider : node.getInputProviders()) {
            Object value;
            try {
                value = inputProvider.getValue(dataContext);
            } catch (IllegalStateException e) {
                return false;
            }

            // --- RUNTIME TYPE VALIDATION ---
            // This check must be performed here, inside execute(), and not in validate().
            // The validate() method only checks the static structure of the graph blueprint.
            // It cannot know the runtime type of a value that comes from a ParentNode
            // or a JsonInputNode, as those are only resolved during execution when the
            // dataContext is available.
            if (!(value instanceof Boolean)) {
                throw new IllegalArgumentException(
                        "AND operation for node '" + node.getNodeName() + "' requires boolean inputs, but got a " + (value == null ? "null" : value.getClass().getSimpleName())
                );
            }

            if (!((Boolean) value)) {
                return false;
            }
        }

        return true;
    }
}
