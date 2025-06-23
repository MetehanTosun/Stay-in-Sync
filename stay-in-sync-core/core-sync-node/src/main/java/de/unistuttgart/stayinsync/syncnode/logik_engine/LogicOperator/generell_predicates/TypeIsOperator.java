package de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.generell_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.ConstantNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.Operation;

import java.util.*;

public class TypeIsOperator implements Operation {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "string", "number", "boolean", "date", "map", "stack", "array"
    );


    /**
     * Validates that the LogicNode is correctly configured for the TYPE_IS operation.
     * <p>
     * This operation requires exactly two inputs:
     * <ol>
     *     <li>An {@link InputNode} that provides the value to be checked.</li>
     *     <li>A {@link ConstantNode} specifying the expected type as a String. The value
     *     of this constant must be one of the allowed type names (e.g., "number", "string").</li>
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
                    "TYPE_IS operation for node '" + node.getNodeName() + "' requires exactly 2 inputs, but got " + (inputs == null ? 0 : inputs.size())
            );
        }

        // Check for the SECOND input: It must be a ConstantNode.
        if (!(inputs.get(1) instanceof ConstantNode)) {
            throw new IllegalArgumentException(
                    "TYPE_IS operation requires the second input (the expected type) to be a ConstantNode."
            );
        }

        // Additional validation: The value of the constant must be a valid type string.
        ConstantNode typeConstant = (ConstantNode) inputs.get(1);
        Object typeValue = typeConstant.getValue(null); // dataContext doesn't matter for constants

        if (!(typeValue instanceof String)) {
            throw new IllegalArgumentException(
                    "The ConstantNode for TYPE_IS must contain a String value, but got " + typeValue.getClass().getSimpleName()
            );
        }

        String typeString = ((String) typeValue).toLowerCase();
        if (!ALLOWED_TYPES.contains(typeString)) {
            throw new IllegalArgumentException(
                    "Invalid type '" + typeString + "' specified for TYPE_IS operation. Allowed types are: " + ALLOWED_TYPES
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