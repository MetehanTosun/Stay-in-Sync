package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.generell_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.ConstantNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MatchesSchemaOperator implements Operation {

    // A lazily-initialized, thread-safe holder for the ObjectMapper instance.
    // We use a simple volatile variable, as CDI.current() is thread-safe.
    private volatile ObjectMapper objectMapper;

    /**
     * Validates that the LogicNode is correctly configured for the MATCHES_SCHEMA operation.
     * <p>
     * This operation requires exactly two inputs:
     * <ol>
     *     <li>The first input provides the JSON document (as a {@link JsonNode}) to be validated.</li>
     *     <li>The second input must be a {@link ConstantNode} containing a pre-compiled
     *     {@link com.networknt.schema.JsonSchema} object.</li>
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
                    "MATCHES_SCHEMA operation for node '" + node.getNodeName() + "' requires exactly 2 inputs: the JSON document and the pre-compiled schema."
            );
        }

        if (!(inputs.get(1) instanceof ConstantNode)) {
            throw new IllegalArgumentException(
                    "MATCHES_SCHEMA operation requires the second input (the schema) to be a ConstantNode."
            );
        }

        ConstantNode schemaConstant = (ConstantNode) inputs.get(1);
        Object schemaValue = schemaConstant.getValue(Collections.emptyMap());
        if (!(schemaValue instanceof JsonSchema) && !(schemaValue instanceof String)) {
            throw new IllegalArgumentException(
                    "The ConstantNode for MATCHES_SCHEMA must contain a com.networknt.schema.JsonSchema object."
            );
        }
    }

    /**
     * Executes the JSON Schema validation.
     * <p>
     * It retrieves the document to be validated from the first input and the pre-compiled
     * schema from the second. Since the engine now operates natively with Jackson's
     * {@link JsonNode}, no type conversion is necessary.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context, now containing Jackson JsonNodes.
     * @return {@code true} if the document is valid against the schema, {@code false} otherwise.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        // 1. Get inputs and the pre-compiled schema.
        InputNode documentProvider = node.getInputProviders().get(0);
        ConstantNode schemaProvider = (ConstantNode) node.getInputProviders().get(1);
        JsonSchema schema = (JsonSchema) schemaProvider.getValue(dataContext);

        // 2. Get the Jackson JsonNode to validate.
        Object documentToValidate;
        try {
            documentToValidate = documentProvider.getValue(dataContext);
        } catch (IllegalStateException e) {
            return false; // Document not found, so it cannot be valid.
        }

        if (!(documentToValidate instanceof JsonNode)) {
            return false; // The value is not a JSON structure.
        }
        JsonNode jacksonDocument = (JsonNode) documentToValidate;

        // 3. Perform the validation.
        Set<ValidationMessage> errors = schema.validate(jacksonDocument);

        // 4. The document is valid if there are no validation errors.
        return errors.isEmpty();
    }
}
