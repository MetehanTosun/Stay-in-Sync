package de.unistuttgart.stayinsync.syncnode.logik_engine.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.LogicOperator;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.ConstantNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.Node;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
    /**
     * Prepares a raw graph loaded from the database for execution.
     * This method iterates through the nodes and replaces specific string constants
     * with compiled runtime objects, such as a {@link JsonSchema}.
     *
     * @param rawGraph The list of Nodes as loaded from the database.
     * @return The same list of Nodes, but now "hydrated" and ready for execution.
     * @throws RuntimeException if schema compilation fails.
     */
    public List<Node> compile(List<Node> rawGraph) {
        // Iterate over each node in the graph to find those that need compilation.
        for (Node node : rawGraph) {
            // Only LogicNodes can have a MATCHES_SCHEMA operator.
            if (node instanceof LogicNode) {
                LogicNode logicNode = (LogicNode) node;

                if (logicNode.getOperator() == LogicOperator.MATCHES_SCHEMA) {
                    // By convention, the schema definition is the second input.
                    // We now access the input via the new inputNodes list.
                    Node schemaInputNode = logicNode.getInputNodes().get(1);

                    // Check if the input is a ConstantNode holding a schema as a String.
                    if (schemaInputNode instanceof ConstantNode) {
                        ConstantNode schemaConstant = (ConstantNode) schemaInputNode;
                        Object schemaValue = schemaConstant.getValue();

                        if (schemaValue instanceof String) {
                            String schemaJsonString = (String) schemaValue;
                            try {
                                // Compile the schema string into a performant, executable JsonSchema object.
                                JsonNode schemaJacksonNode = jacksonMapper.readTree(schemaJsonString);
                                JsonSchema compiledSchema = schemaFactory.getSchema(schemaJacksonNode);

                                // Create a new ConstantNode to hold the compiled object.
                                // We use the new constructor and get the name from the original constant.
                                ConstantNode compiledSchemaConstant = new ConstantNode(
                                        schemaConstant.getName(),
                                        compiledSchema
                                );

                                // Replace the old string-based constant with the new, compiled one in the graph.
                                logicNode.getInputNodes().set(1, compiledSchemaConstant);

                            } catch (Exception e) {
                                throw new RuntimeException("Failed to compile JSON schema for node '" + logicNode.getName() + "': " + e.getMessage(), e);
                            }
                        }
                    }
                }
            }
        }
        return rawGraph;
    }
}
