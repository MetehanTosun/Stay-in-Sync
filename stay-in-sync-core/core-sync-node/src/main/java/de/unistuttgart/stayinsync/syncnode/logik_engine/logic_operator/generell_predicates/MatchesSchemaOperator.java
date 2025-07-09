package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.generell_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.ConstantNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.Node;

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
     * This validation is performed early to ensure the graph is structurally sound
     * before execution begins.
     *
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the node's configuration is invalid.
     */
    @Override
    public void validate(LogicNode node) {
        List<Node> inputs = node.getInputNodes();

        if (inputs == null || inputs.size() != 2) {
            throw new IllegalArgumentException(
                    "MATCHES_SCHEMA operation for node '" + node.getName() + "' requires exactly 2 inputs."
            );
        }

        Node schemaInputNode = inputs.get(1);
        if (!(schemaInputNode instanceof ConstantNode)) {
            throw new IllegalArgumentException(
                    "MATCHES_SCHEMA operation requires the second input to be a ConstantNode."
            );
        }

        // This is your original, more robust check, adapted for the new model.
        // It inspects the configured value of the ConstantNode before execution.
        ConstantNode schemaConstant = (ConstantNode) schemaInputNode;
        Object schemaValue = schemaConstant.getValue();

        // The ConstantNode must contain either the schema String (before compilation)
        // or the compiled JsonSchema object (after compilation).
        if (!(schemaValue instanceof JsonSchema) && !(schemaValue instanceof String)) {
            throw new IllegalArgumentException(
                    "The ConstantNode for MATCHES_SCHEMA must contain a JsonSchema object or a schema String."
            );
        }
    }

    /**
     * Executes the JSON Schema validation on the pre-calculated values of its inputs.
     *
     * @param node        The LogicNode being evaluated.
     * @param dataContext The runtime data context.
     * @return {@code true} if the document is valid against the schema, {@code false} otherwise.
     */
    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext) {
        // 1. Get the provided values from the input nodes.
        Object documentToValidate = node.getInputNodes().get(0).getCalculatedResult();
        Object schemaObject = node.getInputNodes().get(1).getCalculatedResult();

        // 2. The validate() method has already ensured the types are likely correct,
        // but we perform a final runtime check for safety.
        if (!(documentToValidate instanceof JsonNode)) {
            return false;
        }
        if (!(schemaObject instanceof JsonSchema)) {
            return false;
        }

        JsonNode jacksonDocument = (JsonNode) documentToValidate;
        JsonSchema schema = (JsonSchema) schemaObject;

        // 3. Perform the validation.
        Set<ValidationMessage> errors = schema.validate(jacksonDocument);
        return errors.isEmpty();
    }
}
