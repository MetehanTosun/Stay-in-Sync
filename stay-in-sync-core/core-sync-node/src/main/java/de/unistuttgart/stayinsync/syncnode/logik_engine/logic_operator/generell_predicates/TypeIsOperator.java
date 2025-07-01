package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.generell_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.ConstantNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;

import java.util.*;

public class TypeIsOperator implements Operation {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "string", "number", "boolean", "date", "map", "stack", "array"
    );


    /**
     * Validates that the LogicNode is correctly configured for the TYPE_IS operation.
     * <p>
     * This operation checks if one or more values all have a specified type.
     * It requires a minimum of two inputs:
     * <ol>
     *     <li>**Input 0:** A {@link ConstantNode} specifying the expected type as a String
     *         (e.g., "number", "string").</li>
     *     <li>**Input 1...N:** One or more {@link InputNode}s that provide the values to check.</li>
     * </ol>
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node's configuration is invalid.
     */
    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();

        // Rule 1: Must have at least 2 inputs.
        if (inputs == null || inputs.size() < 2) {
            throw new IllegalArgumentException(
                    "TYPE_IS operation for node '" + node.getNodeName() + "' requires at least 2 inputs: a type constant and at least one value to check."
            );
        }

        // Rule 2: The first input must be a ConstantNode.
        if (!(inputs.get(0) instanceof ConstantNode)) {
            throw new IllegalArgumentException(
                    "TYPE_IS operation requires the first input (the expected type) to be a ConstantNode."
            );
        }

        // Rule 3: The value of the constant must be a valid type string.
        ConstantNode typeConstant = (ConstantNode) inputs.get(0);
        Object typeValue = typeConstant.getValue(null);

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
     * @return {@code true} if the value from the first input matches the expected type,
     *         otherwise {@code false}.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        List<InputNode> inputs = node.getInputProviders();

        // 1. Get the expected type from the FIRST input.
        ConstantNode typeConstant = (ConstantNode) inputs.get(0);
        String expectedType = ((String) typeConstant.getValue(dataContext)).toLowerCase();

        // 2. Iterate through the remaining inputs (the values to be checked).
        for (int i = 1; i < inputs.size(); i++) {
            InputNode valueProvider = inputs.get(i);
            Object actualValue;
            try {
                actualValue = valueProvider.getValue(dataContext);
            } catch (IllegalStateException e) {
                return false; // Value not found -> type doesn't match
            }

            if (actualValue == null) {
                return false; // Null has no type
            }

            String actualType = getJavaTypeAsString(actualValue);

            // Early Exit: If even one value doesn't have the expected type, the result is false.
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
        if (obj instanceof String) return "string";
        if (obj instanceof Number) return "number";
        if (obj instanceof Boolean) return "boolean";
        if (obj instanceof Date) return "date";
        if (obj instanceof Map<?,?>) return "map";
        if (obj instanceof Stack<?>) return "stack";
        if (obj instanceof List) return "array";
        return "unknown";
    }
}