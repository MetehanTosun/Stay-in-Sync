package de.unistuttgart.stayinsync.syncnode.logik_engine.Database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import de.unistuttgart.stayinsync.syncnode.logik_engine.ConstantNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.LogicOperator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.List;

/**
 * A service responsible for preparing a "raw" logic graph for execution.
 * <p>
 * This service "hydrates" the graph by converting static definitions (like a
 * JSON schema stored as a string) into compiled, runtime-ready Java objects.
 */
@ApplicationScoped
public class GraphCompilerService {

    private final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(VersionFlag.V7);

    @Inject // Quarkus provides a configured ObjectMapper via CDI.
    ObjectMapper jacksonMapper;

    /**
     * Prepares a raw graph loaded from the database for execution.
     * This method iterates through the nodes and replaces specific string constants
     * with compiled runtime objects, such as a {@link JsonSchema}.
     *
     * @param rawGraph The list of LogicNodes as loaded from the database.
     * @return The same list of LogicNodes, but now "hydrated" and ready for execution.
     * @throws RuntimeException if schema compilation fails.
     */
    public List<LogicNode> compile(List<LogicNode> rawGraph) {
        // Iterate over each node in the graph to find those that need compilation.
        for (LogicNode node : rawGraph) {
            if (node.getOperator() == LogicOperator.MATCHES_SCHEMA) {
                // By convention, the schema definition is the second input.
                InputNode schemaInput = node.getInputProviders().get(1);

                // Only process ConstantNodes that contain a String, which is the expected
                // format for a stored schema definition.
                if (schemaInput instanceof ConstantNode && ((ConstantNode) schemaInput).getValue(Collections.emptyMap()) instanceof String) {

                    ConstantNode schemaStringConstant = (ConstantNode) schemaInput;
                    String schemaJsonString = (String) schemaStringConstant.getValue(Collections.emptyMap());

                    try {
                        // Compile the schema string into a performant, executable JsonSchema object.
                        JsonNode schemaJacksonNode = jacksonMapper.readTree(schemaJsonString);
                        JsonSchema compiledSchema = schemaFactory.getSchema(schemaJacksonNode);

                        // Create a new ConstantNode to hold the compiled object.
                        ConstantNode compiledSchemaConstant = new ConstantNode(
                                schemaStringConstant.getElementName(),
                                compiledSchema
                        );

                        // Replace the old string-based constant with the new, compiled one in the graph.
                        node.getInputProviders().set(1, compiledSchemaConstant);

                    } catch (Exception e) {
                        throw new RuntimeException("Failed to compile JSON schema for node '" + node.getNodeName() + "': " + e.getMessage(), e);
                    }
                }
            }
        }
        return rawGraph;
    }
}
