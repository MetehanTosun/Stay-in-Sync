package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.generell_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.ConstantNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.Node;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;
import java.time.temporal.TemporalAccessor;
import java.util.*;

public class TypeIsOperator implements Operation {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "string", "number", "boolean", "map", "collection"
    );

    /**
     * Validates that the LogicNode is correctly configured for the TYPE_IS operation.
     * It requires at least two inputs: a ConstantNode specifying the type, and one or more nodes to check.
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node's configuration is invalid.
     */
    @Override
    public void validate(LogicNode node) {
        List<Node> inputs = node.getInputNodes();

        if (inputs == null || inputs.size() < 2) {
            throw new IllegalArgumentException(
                    "TYPE_IS operation for node '" + node.getName() + "' requires at least 2 inputs: a type constant and at least one value to check."
            );
        }

        if (!(inputs.get(0) instanceof ConstantNode)) {
            throw new IllegalArgumentException(
                    "TYPE_IS operation requires the first input (the expected type) to be a ConstantNode."
            );
        }

        ConstantNode typeConstant = (ConstantNode) inputs.get(0);
        Object typeValue = typeConstant.getValue();

        if (!(typeValue instanceof String)) {
            throw new IllegalArgumentException(
                    "The ConstantNode for TYPE_IS must contain a String, but found " + (typeValue == null ? "null" : typeValue.getClass().getName())
            );
        }

        String typeString = ((String) typeValue).toLowerCase();
        if (!ALLOWED_TYPES.contains(typeString)) {
            throw new IllegalArgumentException(
                    "Invalid type '" + typeString + "' specified. Allowed types are: " + ALLOWED_TYPES
            );
        }
    }

    /**
     * Executes the type check.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if all provided values match the expected type, otherwise {@code false}.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<Node> inputs = node.getInputNodes();

        Object expectedTypeValue = inputs.get(0).getCalculatedResult();
        String expectedType = ((String) expectedTypeValue).toLowerCase();

        // 2. Iterate through the remaining inputs (the values to be checked).
        for (int i = 1; i < inputs.size(); i++) {
            Node valueNode = inputs.get(i);

            Object actualValue = valueNode.getCalculatedResult();
            String actualType = getJavaTypeAsString(actualValue);

            // Early Exit: If any value doesn't have the expected type, the result is false.
            if (!expectedType.equals(actualType)) {
                return false;
            }
        }
        // If the loop completes, all values had the correct type.
        return true;
    }

    /**
     * Private helper to map a Java object to the simple type names.
     */
    private String getJavaTypeAsString(Object obj) {
        if (obj == null) return "null"; // A specific type for null
        if (obj instanceof String) return "string";
        if (obj instanceof Number) return "number";
        if (obj instanceof Boolean) return "boolean";
        if (obj instanceof Map<?,?>) return "map";
        if (obj.getClass().isArray() || obj instanceof Collection<?>) return "collection"; // Covers List, Set, Stack, and arrays
        return "unknown";
    }
}