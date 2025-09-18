package de.unistuttgart.graphengine.logic_operator.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import de.unistuttgart.graphengine.exception.GraphEvaluationException;
import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.Operation;
import de.unistuttgart.graphengine.nodes.LogicNode;

import de.unistuttgart.graphengine.service.SchemaCache;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.Set;

/**
 * Operation that validates a JSON document against a JSON Schema.
 * <p>
 * This operator takes two inputs:
 * <ul>
 *     <li>First input: A JSON document (as JsonNode) to be validated</li>
 *     <li>Second input: A JSON Schema definition (as String)</li>
 * </ul>
 * <p>
 * The operation returns {@code true} if the JSON document conforms to the provided
 * schema, and {@code false} otherwise. Schema compilation is optimized through caching
 * to avoid repeated parsing of identical schema strings.
 * <p>
 * This implementation uses the JSON Schema Draft 7 specification for validation.
 *
 * @see <a href="https://json-schema.org/">JSON Schema Specification</a>
 * @since 1.0.0
 */
public class MatchesSchemaOperator implements Operation {

    /**
     * Cache service for compiled JSON schemas to improve performance by avoiding
     * repeated compilation of identical schema strings.
     */
    @Inject
    SchemaCache schemaCache;

    /**
     * Validates that the LogicNode is correctly configured for the MATCHES_SCHEMA operation.
     * <p>
     * This method performs structural validation to ensure the node has exactly two inputs
     * before the graph execution begins. It does not validate the actual content types
     * of the inputs, as those are checked during runtime execution.
     * <p>
     * Requirements:
     * <ul>
     *     <li>The node must have exactly 2 input connections</li>
     *     <li>First input should provide a JSON document</li>
     *     <li>Second input should provide a JSON Schema string</li>
     * </ul>
     *
     * @param node The LogicNode to validate for structural correctness
     * @throws OperatorValidationException if the node does not have exactly 2 inputs
     */
    @Override
    public void validateNode(LogicNode node) throws OperatorValidationException {
        if (node.getInputNodes() == null || node.getInputNodes().size() != 2) {
            throw new OperatorValidationException(
                    "MATCHES_SCHEMA operator for node '" + node.getName() +
                            "' requires exactly 2 inputs: a JSON object and a schema string."
            );
        }
    }

    /**
     * Executes the JSON Schema validation operation.
     * <p>
     * This method retrieves the pre-calculated results from both input nodes and performs
     * schema validation. The first input must be a JsonNode representing the document
     * to validate, and the second input must be a String containing the JSON Schema
     * definition.
     * <p>
     * The schema string is compiled into a JsonSchema object using a cached compilation
     * service to optimize performance for repeated validations with the same schema.
     * <p>
     * Validation behavior:
     * <ul>
     *     <li>Returns {@code true} if the JSON document is valid according to the schema</li>
     *     <li>Returns {@code false} if validation fails or if inputs are of incorrect types</li>
     *     <li>Returns {@code false} if either input is null</li>
     * </ul>
     *
     * @param node The LogicNode being evaluated, containing references to input nodes
     * @param dataContext The runtime data context (may be null, not used in this operation)
     * @return {@code Boolean.TRUE} if the document validates against the schema,
     *         {@code Boolean.FALSE} otherwise
     * @throws GraphEvaluationException if schema compilation fails or validation encounters
     *                                  an unexpected error during execution
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) throws GraphEvaluationException {
        // Extract the calculated results from both input nodes
        Object jsonInput = node.getInputNodes().get(0).getCalculatedResult();
        Object schemaInput = node.getInputNodes().get(1).getCalculatedResult();

        // Validate input types at runtime
        if (!(jsonInput instanceof JsonNode)) {
            Log.warnf("Node '%s': First input for MATCHES_SCHEMA must be a JSON object, got %s",
                    node.getName(), jsonInput != null ? jsonInput.getClass().getSimpleName() : "null");
            return false;
        }

        if (!(schemaInput instanceof String)) {
            Log.warnf("Node '%s': Second input for MATCHES_SCHEMA must be a schema string, got %s",
                    node.getName(), schemaInput != null ? schemaInput.getClass().getSimpleName() : "null");
            return false;
        }

        JsonNode jsonData = (JsonNode) jsonInput;
        String schemaString = (String) schemaInput;

        try {
            // Use cached compilation for performance optimization
            JsonSchema compiledSchema = schemaCache.getCompiledSchema(schemaString);

            // Perform the actual validation
            Set<ValidationMessage> validationErrors = compiledSchema.validate(jsonData);

            // Return true if no validation errors were found
            boolean isValid = validationErrors.isEmpty();

            if (!isValid && Log.isDebugEnabled()) {
                Log.debugf("Node '%s': JSON validation failed with %d errors: %s",
                        node.getName(), validationErrors.size(), validationErrors);
            }

            return isValid;

        } catch (Exception e) {
            Log.errorf(e, "Node '%s': Failed to process JSON schema validation", node.getName());
            throw new GraphEvaluationException(
                    GraphEvaluationException.ErrorType.EXECUTION_FAILED,
                    "Schema Validation Error",
                    "Failed to parse or apply the JSON schema for node '" + node.getName() + "'",
                    e
            );
        }
    }

    /**
     * Returns the Java type that this operation produces as output.
     * <p>
     * The MATCHES_SCHEMA operation always returns a boolean value indicating
     * whether the JSON document validates against the provided schema.
     *
     * @return {@code Boolean.class} as this operation returns true/false results
     */
    @Override
    public Class<?> getReturnType() {
        return Boolean.class;
    }
}